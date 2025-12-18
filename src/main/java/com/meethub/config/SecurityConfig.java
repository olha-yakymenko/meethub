
package com.meethub.config;

import com.meethub.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("️  Configuring SecurityFilterChain...");

        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    log.info(" Configuring authorization rules...");
                    auth
                            .requestMatchers(
                                    "/", "/home", "/meetings", "/meetings/**",
                                    "/css/**", "/js/**", "/images/**",
                                    "/webjars/**", "/favicon.ico", "/error", "/swagger-ui/**"
                                    ).permitAll()
                            .requestMatchers("/login", "/register").permitAll()
                            .requestMatchers("/api/v1/auth/**").permitAll()
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .formLogin(form -> {
                    log.info("📝 Configuring form login...");
                    form
                            .loginPage("/login")
                            .loginProcessingUrl("/login")
                            .usernameParameter("email")
                            .passwordParameter("password")
                            .defaultSuccessUrl("/meetings", true)
                            .failureUrl("/login?error=true")
                            .permitAll();
                })

                .logout(logout -> {
                    log.info("Configuring logout...");
                    logout
                            .logoutUrl("/logout")  // ✅ POST /logout
                            .logoutSuccessUrl("/login?logout=true")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll();
                })
                .sessionManagement(session -> {
                    log.info(" Configuring session management...");
                    session
                            .maximumSessions(1)
                            .maxSessionsPreventsLogin(false);
                })
                .authenticationProvider(authenticationProvider());

        log.info(" SecurityFilterChain configuration complete!");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info(" Creating BCryptPasswordEncoder...");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        log.info(" Creating DaoAuthenticationProvider...");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        authProvider.setHideUserNotFoundExceptions(false);

        return authProvider;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}