//package security;
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.security.CustomUserDetailsService;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.test.context.support.WithSecurityContextFactory;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class WithCustomUserSecurityContextFactory
//        implements WithSecurityContextFactory<WithCustomUser> {
//
//    @Override
//    public SecurityContext createSecurityContext(WithCustomUser annotation) {
//        User user = User.builder()
//                .id(annotation.id())
//                .email(annotation.email())
//                .build();
//
//        CustomUserDetailsService.CustomUserDetails principal =
//                new CustomUserDetailsService.CustomUserDetails(user);
//
//        // ustaw role na podstawie adnotacji
//        List<GrantedAuthority> authorities = Arrays.stream(annotation.roles())
//                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//                .collect(Collectors.toList());
//
//        Authentication authentication =
//                new UsernamePasswordAuthenticationToken(principal, null, authorities);
//
//        SecurityContext context = SecurityContextHolder.createEmptyContext();
//        context.setAuthentication(authentication);
//        return context;
//    }
//
//}



//
//package security;
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.UserRole;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.test.context.support.WithSecurityContextFactory;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class WithCustomUserSecurityContextFactory
//        implements WithSecurityContextFactory<WithCustomUser> {
//
//    @Override
//    public SecurityContext createSecurityContext(WithCustomUser annotation) {
//        // Dla @AuthenticationPrincipal Long userId, musimy zwrócić Long, nie CustomUserDetails
//        Long userId = annotation.id();
//
//        // Stwórz uproszczone UserDetails (tylko username) z właściwym ID jako principal
//        org.springframework.security.core.userdetails.User userDetails =
//                new org.springframework.security.core.userdetails.User(
//                        annotation.email(),
//                        "password",
//                        true,
//                        true,
//                        true,
//                        true,
//                        Arrays.stream(annotation.roles())
//                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//                                .collect(Collectors.toList())
//                );
//
//        // Użyj userId jako principal zamiast UserDetails
//        Authentication authentication =
//                new UsernamePasswordAuthenticationToken(
//                        userId, // ← TO JEST KLUCZOWE! Zwracamy Long jako principal
//                        null,
//                        userDetails.getAuthorities()
//                );
//
//        SecurityContext context = SecurityContextHolder.createEmptyContext();
//        context.setAuthentication(authentication);
//        return context;
//    }
//}






package security;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.security.CustomUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WithCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithCustomUser annotation) {
        // Stwórz pełny obiekt User
        User user = User.builder()
                .id(annotation.id())
                .email(annotation.email())
//                .firstName(annotation.firstName())
//                .lastName(annotation.lastName())
//                .password(annotation.password())
                .enabled(true)
                .role(UserRole.valueOf(annotation.roles()[0])) // Pierwsza rola jako główna
                .build();

        // Stwórz CustomUserDetails
        CustomUserDetailsService.CustomUserDetails customUserDetails =
                new CustomUserDetailsService.CustomUserDetails(user);

        // Utwórz authorities
        List<GrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // KLUCZOWE: Zwracamy Authentication z DWIEMA możliwościami:
        // 1. Principal = customUserDetails (dla kontrolerów używających CustomUserDetails)
        // 2. Dodatkowo ustawiamy userId jako name w authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails, // principal - dla kontrolerów z @AuthenticationPrincipal CustomUserDetails
                "password",        // credentials
                authorities
        );

//        // DODATKOWO: Ustaw userId jako dodatkową właściwość, żeby można było go wyciągnąć
//        authentication.setDetails(annotation.id()); // userId w details

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}