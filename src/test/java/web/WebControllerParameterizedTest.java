package com.meethub.controller.web;

import com.meethub.domain.model.entity.User;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import security.WithCustomUser;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class WebControllerParameterizedTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "5", "10"})
    @WithCustomUser(id = 1L, email = "user@example.com")
    void meetings_shouldHandleDifferentPageNumbers(int pageNumber) throws Exception {

        mockMvc.perform(get("/meetings")
                        .param("page", String.valueOf(pageNumber))
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/list"))
                .andExpect(model().attributeExists("meetings"));
    }

    @ParameterizedTest
    @CsvSource({
            "3, true",
            "10, true",
            "50, true"
    })
    @WithCustomUser(id = 1L, email = "user@example.com")
    void meetings_shouldValidatePageSize(int size, boolean shouldPass) throws Exception {

        var request = get("/meetings")
                .param("page", "0")
                .param("size", String.valueOf(size));

        if (shouldPass) {
            mockMvc.perform(request)
                    .andExpect(status().isOk());
        } else {
            mockMvc.perform(request)
                    .andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @MethodSource("searchParametersProvider")
    @WithCustomUser(id = 1L, email = "user@example.com")
    void searchMeetings_shouldHandleDifferentParameters(String search, String type, String status) throws Exception {


        mockMvc.perform(get("/meetings/search")
                        .param("search", search)
                        .param("type", type)
                        .param("status", status)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/advanced-search"))
                .andExpect(model().attributeExists("meetings"));
    }

    private static Stream<Arguments> searchParametersProvider() {
        return Stream.of(
                Arguments.of("conference", "WORKSHOP", "SCHEDULED"),
                Arguments.of("", "ONLINE", "CANCELLED"),
                Arguments.of("test", null, null),
                Arguments.of(null, "IN_PERSON", "COMPLETED"),
                Arguments.of("meeting", "", "")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"startDate", "title", "createdAt", "updatedAt"})
    @WithCustomUser(id = 1L, email = "user@example.com")
    void searchMeetings_shouldHandleDifferentSortFields(String sortBy) throws Exception {


        mockMvc.perform(get("/meetings/search")
                        .param("sortBy", sortBy)
                        .param("sortOrder", "asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/advanced-search"));
    }
}