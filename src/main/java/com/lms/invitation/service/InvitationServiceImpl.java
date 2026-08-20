package com.lms.invitation.service;

import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.mail.MailMessage;
import com.lms.common.mail.MailSender;
import com.lms.common.response.PageResponse;
import com.lms.common.util.PageRequests;
import com.lms.common.util.TemporaryPasswordGenerator;
import com.lms.common.util.TokenGenerator;
import com.lms.common.util.TokenHasher;
import com.lms.config.AuthPolicyConfig;
import com.lms.invitation.dto.request.CreateInvitationRequest;
import com.lms.invitation.dto.response.InvitationResponse;
import com.lms.invitation.entity.Invitation;
import com.lms.invitation.mapper.InvitationMapper;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.role.entity.Role;
import com.lms.role.service.RoleService;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.AccountStatus;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.user.service.AccountStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationServiceImpl implements InvitationService {

    private static final String RESOURCE = "INVITATION";

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AccountStatusService accountStatusService;
    private final InvitationMapper invitationMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final AuditService auditService;
    private final AuthPolicyConfig policy;

    @Override
    @Transactional
    public InvitationResponse invite(CreateInvitationRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ResourceAlreadyExistsException.of("User", email);
        }

        Role role = roleService.requireByName(request.getRole());
        UUID actorId = currentActorId();

        // The account is created with a temporary password and is active, so the
        // invitee can sign in immediately. The pending invitation row below is
        // what forces them to replace that password before doing anything else.
        String temporaryPassword = TemporaryPasswordGenerator.generate();

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(temporaryPassword))
                .active(true)
                .locked(false)
                .build();

        User saved = userRepository.save(user);
        saved.assignRole(role, actorId);

        accountStatusService.recordTransition(saved.getId(), AccountStatus.ACTIVE, actorId,
                "Account created by invitation");

        // Still minted and stored: the ERD has token_hash NOT NULL, and this is
        // what a re-introduced set-password link would redeem. Nothing is sent
        // anywhere, so it is inert while the link route is off.
        String rawToken = TokenGenerator.urlSafeToken();

        Invitation invitation = Invitation.builder()
                .user(saved)
                .tokenHash(TokenHasher.sha256(rawToken))
                .invitedBy(actorId == null ? null : userRepository.getReferenceById(actorId))
                .expiresAt(Instant.now().plus(policy.getInvitationTtl()))
                .build();

        Invitation persisted = invitationRepository.save(invitation);

        sendInvitationMail(saved, role.getName(), temporaryPassword);

        auditService.record(actorId, AuditAction.USER_INVITED, RESOURCE, persisted.getId(),
                "Invited " + email + " as " + role.getName());

        log.info("Invitation issued for {} as {}", email, role.getName());
        return invitationMapper.toResponse(persisted);
    }

    @Override
    public PageResponse<InvitationResponse> findAll(Pageable pageable) {
        return PageResponse.from(invitationRepository.findAllWithUser(PageRequests.sanitize(pageable)),
                invitationMapper::toResponse);
    }

    @Override
    public InvitationResponse findById(UUID id) {
        return invitationMapper.toResponse(require(id));
    }

    @Override
    @Transactional
    public InvitationResponse resend(UUID id) {
        Invitation invitation = require(id);

        if (invitation.isAccepted()) {
            throw new BusinessRuleException("This invitation has already been accepted");
        }

        // Both credentials are reissued: the old temporary password must stop
        // working at the same moment the old link does.
        String rawToken = TokenGenerator.urlSafeToken();
        String temporaryPassword = TemporaryPasswordGenerator.generate();

        invitation.setTokenHash(TokenHasher.sha256(rawToken));
        invitation.setExpiresAt(Instant.now().plus(policy.getInvitationTtl()));
        invitation.getUser().setPassword(passwordEncoder.encode(temporaryPassword));

        String roleName = invitation.getUser().roleNames().stream().findFirst().orElse("member");
        sendInvitationMail(invitation.getUser(), roleName, temporaryPassword);

        auditService.record(currentActorId(), AuditAction.INVITATION_RESENT, RESOURCE, invitation.getId(), null);

        log.info("Invitation for {} reissued", invitation.getUser().getEmail());
        return invitationMapper.toResponse(invitation);
    }

    @Override
    @Transactional
    public void revoke(UUID id) {
        Invitation invitation = require(id);
        User user = invitation.getUser();

        if (invitation.isAccepted()) {
            throw new BusinessRuleException(
                    "This invitation has been accepted; deactivate the user account instead");
        }

        UUID userId = user.getId();
        String email = user.getEmail();

        // The account exists only because of this invitation and has never been
        // activated, so withdrawing the invitation removes both. The ERD has no
        // revoked flag to set instead.
        invitationRepository.delete(invitation);
        userRepository.delete(user);

        auditService.record(currentActorId(), AuditAction.INVITATION_REVOKED, RESOURCE, id,
                "Revoked invitation for " + email);

        log.info("Invitation for {} revoked and account {} removed", email, userId);
    }

    private Invitation require(UUID id) {
        return invitationRepository.findByIdWithUser(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Invitation", id));
    }

    private void sendInvitationMail(User user, String roleName, String temporaryPassword) {
        String body = """
                Hello %s,

                An LMS account has been created for you as %s.

                Sign in with these details and you will be asked to choose your
                own password straight away:

                    Email:              %s
                    Temporary password: %s

                The temporary password stops working on %s, and the moment you
                set your own it is gone.
                """.formatted(
                user.getName(),
                roleName.toLowerCase(),
                user.getEmail(),
                temporaryPassword,
                Instant.now().plus(policy.getInvitationTtl()));

        mailSender.send(new MailMessage(user.getEmail(), "Your LMS account", body));
    }

    private UUID currentActorId() {
        return AuthenticationService.currentPrincipal().map(LmsUserDetails::getUserId).orElse(null);
    }
}
