package com.meethub.config;

import com.meethub.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ✅ PUBLIC endpoints - dostępne BEZ logowania
                        .requestMatchers(
                                "/",
                                "/home",
                                "/meetings",              // ✅ LISTA spotkań - PUBLICZNA
                                "/meetings/*",            // ✅ SZCZEGÓŁY spotkań - PUBLICZNE
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // ✅ AUTH pages - dostępne BEZ logowania
                        .requestMatchers(
                                "/login",
                                "/register"
                        ).permitAll()

                        // ✅ PROTECTED endpoints - wymagają logowania
                        .requestMatchers(
                                "/dashboard",
                                "/my-meetings/**",
                                "/profile/**",
                                "/meetings/create/**",    // ✅ TWORZENIE spotkań - CHRONIONE
                                "/meetings/*/edit/**",    // ✅ EDYCJA spotkań - CHRONIONE
                                "/meetings/*/delete/**",  // ✅ USUWANIE spotkań - CHRONIONE
                                "/meetings/*/join/**",    // ✅ DOŁĄCZANIE - CHRONIONE
                                "/meetings/*/leave/**"    // ✅ OPUSZCZANIE - CHRONIONE
                        ).authenticated()

                        // ✅ ADMIN endpoints
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                // FORM LOGIN configuration
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/meetings", true)  // ✅ Po logowaniu idź do listy spotkań
                        .failureUrl("/login?error=true")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll()
                )
                // LOGOUT configuration
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/meetings?logout=true")  // ✅ Po wylogowaniu idź do listy spotkań
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}