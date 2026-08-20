package com.lms.security.authentication;

import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Loads user security information for Spring Security.
 *
 * <p>An invited-but-not-activated account has a null password. Reporting it as
 * "not found" keeps the DaoAuthenticationProvider on its constant-time path and
 * avoids leaking that the address is known.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public LmsUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithAuthorities(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));

        if (user.getPassword() == null) {
            throw new UsernameNotFoundException("Account " + email + " has not been activated");
        }
        return LmsUserDetails.from(user);
    }

    @Transactional(readOnly = true)
    public LmsUserDetails loadUserById(UUID userId) {
        User user = userRepository.findByIdWithAuthorities(userId)
                .orElseThrow(() -> new UsernameNotFoundException("No account for id " + userId));
        return LmsUserDetails.from(user);
    }
}
