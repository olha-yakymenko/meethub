//package security;
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.UserRole;
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
//public class WithUserIdSecurityContextFactory
//        implements WithSecurityContextFactory<WithUserId> {
//
//    @Override
//    public SecurityContext createSecurityContext(WithUserId annotation) {
//
//        // Tworzymy obiekt User
//        User user = User.builder()
//                .id(annotation.value())
//                .email("test@test.pl")
//                .enabled(true)
//                .role(UserRole.valueOf(annotation.roles()[0]))
//                .build();
//
//        // Tworzymy CustomUserDetails
//        CustomUserDetailsService.CustomUserDetails principal =
//                new CustomUserDetailsService.CustomUserDetails(user);
//
//        // Tworzymy authorities
//        List<GrantedAuthority> authorities = Arrays.stream(annotation.roles())
//                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//                .collect(Collectors.toList());
//
//        // Tworzymy Authentication
//        UsernamePasswordAuthenticationToken authentication =
//                new UsernamePasswordAuthenticationToken(principal, "password", authorities);
//
//        // KLUCZOWE: ustawiamy userId w details
//        authentication.setDetails(user.getId());
//
//        // Tworzymy SecurityContext
//        SecurityContext context = SecurityContextHolder.createEmptyContext();
//        context.setAuthentication(authentication);
//        return context;
//    }
//}



package security;

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

public class WithUserIdSecurityContextFactory
        implements WithSecurityContextFactory<WithUserId> {

    @Override
    public SecurityContext createSecurityContext(WithUserId annotation) {
        // KLUCZOWE: Zwracamy Long userId jako principal, a nie CustomUserDetails
        Long userId = annotation.value();

        // Tworzymy authorities
        List<GrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Tworzymy Authentication z userId jako principal
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        // Ustawiamy userId również w details dla dodatkowej pewności
        authentication.setDetails(userId);

        // Tworzymy SecurityContext
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
