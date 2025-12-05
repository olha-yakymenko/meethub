//package com.meethub.security;
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.repository.jpa.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.Collection;
//import java.util.Collections;
//
//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
//
//        return new CustomUserDetails(user);
//    }
//
//    public static class CustomUserDetails implements UserDetails {
//        private final User user;
//
//        public CustomUserDetails(User user) {
//            this.user = user;
//        }
//
//        @Override
//        public Collection<? extends GrantedAuthority> getAuthorities() {
//            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
//        }
//
//        @Override
//        public String getPassword() {
//            return user.getPassword();
//        }
//
//        @Override
//        public String getUsername() {
//            return user.getEmail(); // ✅ Używamy bezpośrednio email z User
//        }
//
//        @Override
//        public boolean isAccountNonExpired() {
//            return true;
//        }
//
//        @Override
//        public boolean isAccountNonLocked() {
//            return user.getAccountLockedUntil() == null ||
//                    user.getAccountLockedUntil().isBefore(java.time.LocalDateTime.now());
//        }
//
//        @Override
//        public boolean isCredentialsNonExpired() {
//            return true;
//        }
//
//        @Override
//        public boolean isEnabled() {
//            return user.getEnabled();
//        }
//
//        public Long getId() {
//            return user.getId();
//        }
//
//        public String getFirstName() {
//            return user.getFirstName();
//        }
//
//        public String getLastName() {
//            return user.getLastName();
//        }
//
//        public String getFullName() {
//            return user.getFullName();
//        }
//
//        public User getUser() {
//            return user;
//        }
//    }
//}


//
////dozmain
//
//package com.meethub.security;
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.repository.jpa.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.Collection;
//import java.util.Collections;
//
//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
//
//        return new CustomUserDetails(user);
//    }
//
//    public static class CustomUserDetails implements UserDetails {
//        private final User user;
//
//        public CustomUserDetails(User user) {
//            this.user = user;
//        }
//
//        @Override
//        public Collection<? extends GrantedAuthority> getAuthorities() {
//            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
//        }
//
//        @Override
//        public String getPassword() {
//            return user.getPassword();
//        }
//
//        @Override
//        public String getUsername() {
//            return user.getEmail();
//        }
//
//        @Override
//        public boolean isAccountNonExpired() {
//            return true;
//        }
//
//        @Override
//        public boolean isAccountNonLocked() {
//            return user.getAccountLockedUntil() == null ||
//                    user.getAccountLockedUntil().isBefore(java.time.LocalDateTime.now());
//        }
//
//        @Override
//        public boolean isCredentialsNonExpired() {
//            return true;
//        }
//
//        @Override
//        public boolean isEnabled() {
//            return user.getEnabled();
//        }
//
//        public Long getId() {
//            return user.getId();
//        }
//
//        public String getFirstName() {
//            return user.getFirstName();
//        }
//
//        public String getLastName() {
//            return user.getLastName();
//        }
//    }
//}









package com.meethub.security;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("🔍 SPRING SECURITY: Loading user by username/email: '{}'", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("❌ SPRING SECURITY: User not found with email: '{}'", email);
                        return new UsernameNotFoundException("User not found with email: " + email);
                    });

            log.info("✅ SPRING SECURITY: User found - ID: {}, Name: {} {}, Email: {}, Role: {}",
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole());

            log.info("🔑 SPRING SECURITY: User password hash (first 20 chars): {}...",
                    user.getPassword() != null ?
                            user.getPassword().substring(0, Math.min(20, user.getPassword().length())) : "NULL");

            // Sprawdź czy konto jest zablokowane
            if (user.getAccountLockedUntil() != null &&
                    user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                log.warn("⛔ SPRING SECURITY: Account is locked until: {}",
                        user.getAccountLockedUntil());
                throw new LockedException("Account is locked");
            }

            // Sprawdź czy konto jest aktywne
            if (user.getEnabled() != null && !user.getEnabled()) {
                log.warn("🚫 SPRING SECURITY: Account is disabled");
                throw new DisabledException("Account is disabled");
            }

            return new CustomUserDetails(user);

        } catch (UsernameNotFoundException e) {
            log.error("❌ SPRING SECURITY: Authentication failed - user not found");
            throw e;
        } catch (Exception e) {
            log.error("💥 SPRING SECURITY: Unexpected error loading user", e);
            throw new UsernameNotFoundException("Error loading user", e);
        }
    }

    public static class CustomUserDetails implements UserDetails {
        private final User user;

        public CustomUserDetails(User user) {
            this.user = user;
            log.debug("👤 Created CustomUserDetails for user: {}", user.getEmail());
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            String role = "ROLE_" + user.getRole().name();
            log.debug("🎖️  Granted authority: {}", role);
            return Collections.singletonList(new SimpleGrantedAuthority(role));
        }

        @Override
        public String getPassword() {
            log.debug("🔐 Returning password hash for user: {}", user.getEmail());
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            log.debug("📧 Returning username/email: {}", user.getEmail());
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            boolean result = true;
            log.debug("⏰ Account non-expired: {} for user: {}", result, user.getEmail());
            return result;
        }

        @Override
        public boolean isAccountNonLocked() {
            boolean isLocked = user.getAccountLockedUntil() != null &&
                    user.getAccountLockedUntil().isAfter(LocalDateTime.now());
            boolean result = !isLocked;
            log.debug("🔒 Account non-locked: {} for user: {} (locked until: {})",
                    result, user.getEmail(), user.getAccountLockedUntil());
            return result;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            boolean result = true;
            log.debug("🔑 Credentials non-expired: {} for user: {}", result, user.getEmail());
            return result;
        }

        @Override
        public boolean isEnabled() {
            boolean result = user.getEnabled() != null ? user.getEnabled() : true;
            log.debug("✅ Account enabled: {} for user: {}", result, user.getEmail());
            return result;
        }

        public Long getId() {
            return user.getId();
        }
    }
}