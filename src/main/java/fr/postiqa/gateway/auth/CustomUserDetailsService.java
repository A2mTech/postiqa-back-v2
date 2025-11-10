package fr.postiqa.gateway.auth;

import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation.
 * Loads user from database with roles and permissions for authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        UserEntity user = userRepository.findByEmailWithRolesAndPermissions(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        log.debug("User loaded: {}, roles: {}", email, user.getUserRoles().size());

        return new CustomUserDetails(user);
    }
}
