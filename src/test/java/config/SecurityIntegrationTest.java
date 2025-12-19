// [file name]: SecurityIntegrationTest.java
package com.meethub.config;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testAdmin;
    private User disabledUser;
    private User lockedUser;

    @BeforeEach
    void setUp() {
        // Clear any existing test data
        userRepository.deleteAll();

        // Create test users
        testUser = User.builder()
                .email("user@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Test")
                .lastName("User")
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .build();
        userRepository.save(testUser);

        testAdmin = User.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("Admin123!"))
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ORGANIZER)
                .enabled(true)
                .build();
        userRepository.save(testAdmin);

        disabledUser = User.builder()
                .email("disabled@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Disabled")
                .lastName("User")
                .role(UserRole.PARTICIPANT)
                .enabled(false)
                .build();
        userRepository.save(disabledUser);

        lockedUser = User.builder()
                .email("locked@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Locked")
                .lastName("User")
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .accountLockedUntil(LocalDateTime.now().plusHours(1))
                .build();
        userRepository.save(lockedUser);
    }

    // ========== PUBLIC ENDPOINTS TESTS ==========



    @Test
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "user@test.com")
                        .param("password", "wrongPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void shouldFailLoginWithDisabledAccount() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "disabled@test.com")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void shouldFailLoginWithLockedAccount() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "locked@test.com")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void shouldFailLoginWithNonExistentUser() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "nonexistent@test.com")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }


    // ========== ADMIN ACCESS TESTS ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void shouldDenyUserAccessToAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void shouldRedirectAnonymousToLoginForAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ========== LOGOUT TESTS ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(cookie().maxAge("JSESSIONID", 0));
    }

    @Test
    @WithAnonymousUser
    void shouldHandleLogoutForAnonymousUser() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    // ========== SESSION MANAGEMENT TESTS ==========

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void shouldMaintainSessionAfterMultipleRequests() throws Exception {
        String sessionId = mockMvc.perform(get("/meetings"))
                .andReturn()
                .getRequest()
                .getSession()
                .getId();

        mockMvc.perform(get("/profile"))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", org.hamcrest.Matchers.notNullValue()));
    }

    // ========== CSRF TESTS ==========

    @Test
    @WithAnonymousUser
    void shouldAllowGetRequestsWithoutCsrf() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    void shouldRegisterNewUserSuccessfully() throws Exception {
        mockMvc.perform(post("/register")
                        .param("firstName", "New")
                        .param("lastName", "User")
                        .param("email", "newuser@test.com")
                        .param("password", "NewPassword123!")
                        .param("confirmPassword", "NewPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        Optional<User> savedUser = userRepository.findByEmail("newuser@test.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEnabled()).isTrue();
    }

    @Test
    void shouldFailRegistrationWithExistingEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .param("firstName", "Duplicate")
                        .param("lastName", "User")
                        .param("email", "user@test.com") // Already exists
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().isOk()) // Stays on registration page
                .andExpect(model().attributeExists("error"));
    }


    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void shouldAllowConcurrentSessionForSameUser() throws Exception {
        mockMvc.perform(get("/meetings"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk());

    }

}