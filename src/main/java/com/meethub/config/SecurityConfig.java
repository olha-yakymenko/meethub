//package com.meethub.config;
//
//import com.meethub.security.CustomUserDetailsService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.client.RestTemplate;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final CustomUserDetailsService userDetailsService;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        // ✅ PUBLIC endpoints - dostępne BEZ logowania
//                        .requestMatchers(
//                                "/",
//                                "/home",
//                                "/meetings",              // ✅ LISTA spotkań - PUBLICZNA
//                                "/meetings/*",            // ✅ SZCZEGÓŁY spotkań - PUBLICZNE
//                                "/css/**",
//                                "/js/**",
//                                "/images/**",
//                                "/webjars/**",
//                                "/favicon.ico",
//                                "/error"
//                        ).permitAll()
//
//                        // ✅ AUTH pages - dostępne BEZ logowania
//                        .requestMatchers(
//                                "/login",
//                                "/register"
//                        ).permitAll()
//
//                        // ✅ PROTECTED endpoints - wymagają logowania
//                        .requestMatchers(
//                                "/dashboard",
//                                "/my-meetings/**",
//                                "/profile/**",
//                                "/meetings/create/**",    // ✅ TWORZENIE spotkań - CHRONIONE
//                                "/meetings/*/edit/**",    // ✅ EDYCJA spotkań - CHRONIONE
//                                "/meetings/*/delete/**",  // ✅ USUWANIE spotkań - CHRONIONE
//                                "/meetings/*/join/**",    // ✅ DOŁĄCZANIE - CHRONIONE
//                                "/meetings/*/leave/**"    // ✅ OPUSZCZANIE - CHRONIONE
//                        ).authenticated()
//
//                        // ✅ ADMIN endpoints
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//
//                        .anyRequest().authenticated()
//                )
//                // FORM LOGIN configuration
//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .loginProcessingUrl("/login")
//                        .defaultSuccessUrl("/meetings", true)  // ✅ Po logowaniu idź do listy spotkań
//                        .failureUrl("/login?error=true")
//                        .usernameParameter("email")
//                        .passwordParameter("password")
//                        .permitAll()
//                )
//                // LOGOUT configuration
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/meetings?logout=true")  // ✅ Po wylogowaniu idź do listy spotkań
//                        .invalidateHttpSession(true)
//                        .deleteCookies("JSESSIONID")
//                        .permitAll()
//                )
//                .authenticationProvider(authenticationProvider());
//
//        return http.build();
//    }
//
//    @Bean
//    public DaoAuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(userDetailsService);
//        authProvider.setPasswordEncoder(passwordEncoder());
//        return authProvider;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }
//}







//
//package com.meethub.config;
//
//import com.meethub.security.CustomUserDetailsService;
//import com.meethub.security.JwtAuthenticationFilter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.client.RestTemplate;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final CustomUserDetailsService userDetailsService;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        // ✅ PUBLIC endpoints
//                        .requestMatchers(
//                                "/",
//                                "/home",
//                                "/meetings",
//                                "/meetings/*",
//                                "/css/**",
//                                "/js/**",
//                                "/images/**",
//                                "/webjars/**",
//                                "/favicon.ico",
//                                "/error",
//                                "/login",
//                                "/register",
//                                "/api/v1/auth/**"  // ✅ DODAJ endpointy API auth
//                        ).permitAll()
//
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .loginProcessingUrl("/login")
//                        .defaultSuccessUrl("/meetings", true)
//                        .failureUrl("/login?error=true")
//                        .usernameParameter("email")
//                        .passwordParameter("password")
//                        .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/meetings?logout=true")
//                        .invalidateHttpSession(true)
//                        .deleteCookies("JSESSIONID")
//                        .permitAll()
//                )
//                .authenticationProvider(authenticationProvider());
//
//        return http.build();
//    }
//
//    @Bean
//    public DaoAuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(userDetailsService);
//        authProvider.setPasswordEncoder(passwordEncoder());
//        return authProvider;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        AuthenticationManagerBuilder authenticationManagerBuilder =
//                http.getSharedObject(AuthenticationManagerBuilder.class);
//        authenticationManagerBuilder
//                .userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder());
//        return authenticationManagerBuilder.build();
//    }
//
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }
//}










//
////do zmian
//
//package com.meethub.config;
//
//import com.meethub.security.CustomUserDetailsService;
//import com.meethub.security.JwtAuthenticationFilter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.client.RestTemplate;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final CustomUserDetailsService userDetailsService;
////    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(
//                                "/",
//                                "/home",
//                                "/meetings",
//                                "/meetings/*",
//                                "/css/**",
//                                "/js/**",
//                                "/images/**",
//                                "/webjars/**",
//                                "/favicon.ico",
//                                "/error",
//                                "/login",
//                                "/register",
//                                "/api/v1/auth/**"  // ✅ DODAJ endpointy API auth
//                        ).permitAll()
//
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .loginProcessingUrl("/login")
//                        .defaultSuccessUrl("/meetings", true)
//                        .failureUrl("/login?error=true")
//                        .usernameParameter("email")
//                        .passwordParameter("password")
//                        .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/meetings?logout=true")
//                        .invalidateHttpSession(true)
//                        .deleteCookies("JSESSIONID")
//                        .permitAll()
//                )
////                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//                .authenticationProvider(authenticationProvider());
//
//        return http.build();
//    }
//
//    @Bean
//    public DaoAuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(userDetailsService);
//        authProvider.setPasswordEncoder(passwordEncoder());
//        return authProvider;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        AuthenticationManagerBuilder authenticationManagerBuilder =
//                http.getSharedObject(AuthenticationManagerBuilder.class);
//        authenticationManagerBuilder
//                .userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder());
//        return authenticationManagerBuilder.build();
//    }
//
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }
//}
//
//










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
        log.info("🛡️  Configuring SecurityFilterChain...");

        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    log.info("🔐 Configuring authorization rules...");
                    auth
                            .requestMatchers(
                                    "/", "/home", "/meetings", "/meetings/**",
                                    "/css/**", "/js/**", "/images/**",
                                    "/webjars/**", "/favicon.ico", "/error"
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
//                .logout(logout -> {
//                    log.info("🚪 Configuring logout...");
//                    logout
//                            .logoutUrl("/auth/logout")
//                            .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout", "GET"))
//                            .logoutSuccessUrl("/meetings?logout=true")
//                            .invalidateHttpSession(true)
//                            .deleteCookies("JSESSIONID")
//                            .permitAll();
//                })
                .logout(logout -> {
                    log.info("🚪 Configuring logout...");
                    logout
                            .logoutUrl("/logout")  // ✅ POST /logout
                            .logoutSuccessUrl("/login?logout=true")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll();
                })
                .sessionManagement(session -> {
                    log.info("💾 Configuring session management...");
                    session
                            .maximumSessions(1)
                            .maxSessionsPreventsLogin(false);
                })
                .authenticationProvider(authenticationProvider());

        log.info("✅ SecurityFilterChain configuration complete!");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("🔑 Creating BCryptPasswordEncoder...");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        log.info("👤 Creating DaoAuthenticationProvider...");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        // ✅ DODAJ obsługę błędów z logowaniem
        authProvider.setHideUserNotFoundExceptions(false);

        return authProvider;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}