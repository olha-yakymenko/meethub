//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.security.CustomUserDetailsService;
//import com.meethub.security.JwtUtil;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@Transactional
//class AuthServiceIntegrationTest {
//
//    @Autowired
//    private AuthServiceImpl authService;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @MockBean
//    private PasswordEncoder passwordEncoder;
//
//    @MockBean
//    private JwtUtil jwtUtil;
//
//    @MockBean
//    private CustomUserDetailsService userDetailsService;
//
//    @Test
//    void register_ShouldSaveUserToDatabase() {
//        // Given
//        UserRegistrationRequest request = UserRegistrationRequest.builder()
//                .email("integration@test.com")
//                .password("password123")
//                .confirmPassword("password123")
//                .firstName("Integration")
//                .lastName("Test")
//                .phoneNumber("987654321")
//                .build();
//
//        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
//
//        // When
//        var result = authService.register(request);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getEmail()).isEqualTo("integration@test.com");
//
//        // Verify in database
//        User savedUser = userRepository.findByEmail("integration@test.com").orElse(null);
//        assertThat(savedUser).isNotNull();
//        assertThat(savedUser.getFirstName()).isEqualTo("Integration");
//        assertThat(savedUser.getRole()).isEqualTo(UserRole.PARTICIPANT);
//    }
//}