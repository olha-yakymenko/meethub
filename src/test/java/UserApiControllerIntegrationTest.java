// src/test/java/com/meethub/controller/api/UserApiControllerIntegrationTest.java
package com.meethub.controller.api;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb"
})
class UserApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up and create test data
        userRepository.deleteAll();

        User user1 = User.builder()
                .email("jan.kowalski@example.com")
                .firstName("Jan")
                .lastName("Kowalski")
                .password("password123")
                .build();

        User user2 = User.builder()
                .email("anna.nowak@example.com")
                .firstName("Anna")
                .lastName("Nowak")
                .password("password123")
                .build();

        User user3 = User.builder()
                .email("test.user@example.com")
                .firstName("Test")
                .lastName("User")
                .password("password123")
                .build();

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
    }

    @Test
    @WithMockUser
    void shouldSearchUsersByEmail() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "kowalski")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("jan.kowalski@example.com")))
                .andExpect(jsonPath("$[0].firstName", is("Jan")))
                .andExpect(jsonPath("$[0].lastName", is("Kowalski")));
    }

    @Test
    @WithMockUser
    void shouldSearchUsersByFirstName() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "Anna")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName", is("Anna")));
    }

    @Test
    @WithMockUser
    void shouldSearchUsersByLastName() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "Nowak")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastName", is("Nowak")));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyArrayWhenNoUsersFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void shouldReturnMultipleUsersForCommonSearch() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @WithMockUser
    void shouldHandleCaseInsensitiveSearch() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "KOWALSKI")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("jan.kowalski@example.com")));
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}