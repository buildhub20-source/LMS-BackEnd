package com.lms.announcement.service;

import com.lms.announcement.dto.AnnouncementRequest;
import com.lms.announcement.dto.AnnouncementResponse;
import com.lms.announcement.entity.Announcement;
import com.lms.announcement.entity.AnnouncementType;
import com.lms.announcement.repository.AnnouncementRepository;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.notification.service.NotificationService;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    static final String NOTIFICATION_TYPE = "ANNOUNCEMENT";
    private static final Set<String> MANAGE_ANY = Set.of("ADMIN", "SUPER_ADMIN");

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Admins see every announcement; instructors see the ones they wrote. */
    public List<AnnouncementResponse> list(LmsUserDetails actor) {
        List<Announcement> rows = canManageAny(actor)
                ? announcementRepository.findAllByOrderByCreatedAtDesc()
                : announcementRepository.findAllByCreatedByOrderByCreatedAtDesc(actor.getUserId());
        return rows.stream().map(AnnouncementResponse::from).toList();
    }

    /** What students should see right now. */
    public List<AnnouncementResponse> listVisible() {
        return announcementRepository.findVisible(Instant.now()).stream()
                .map(AnnouncementResponse::from)
                .toList();
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request, LmsUserDetails actor) {
        Announcement saved = announcementRepository.save(Announcement.builder()
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .type(request.getType() == null ? AnnouncementType.INFO : request.getType())
                .expiresAt(request.getExpiresAt())
                .active(true)
                .createdBy(actor.getUserId())
                .build());
        notifyStudents(saved);
        log.info("Announcement {} published by {}", saved.getId(), actor.getUserId());
        return AnnouncementResponse.from(saved);
    }

    @Transactional
    public AnnouncementResponse setActive(UUID id, boolean active, LmsUserDetails actor) {
        Announcement announcement = requireManageable(id, actor);
        announcement.setActive(active);
        return AnnouncementResponse.from(announcement);
    }

    @Transactional
    public void delete(UUID id, LmsUserDetails actor) {
        announcementRepository.delete(requireManageable(id, actor));
    }

    private Announcement requireManageable(UUID id, LmsUserDetails actor) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Announcement", id));
        if (!canManageAny(actor) && !actor.getUserId().equals(announcement.getCreatedBy())) {
            throw new AccessDeniedException("You can only change announcements you published");
        }
        return announcement;
    }

    private static boolean canManageAny(LmsUserDetails actor) {
        return actor.getRoles().stream().anyMatch(MANAGE_ANY::contains);
    }

    /** Puts the announcement in every active student's notification bell. */
    private void notifyStudents(Announcement announcement) {
        List<User> students = userRepository.findUsersByRoleName("STUDENT");
        int notified = 0;
        for (User student : students) {
            if (!student.isActive()) continue;
            notificationService.createNotification(student.getId(), NOTIFICATION_TYPE,
                    announcement.getTitle(), announcement.getMessage(), null);
            notified++;
        }
        log.debug("Announcement {} sent to {} students", announcement.getId(), notified);
    }
}
