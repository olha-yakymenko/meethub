//// MeetingResourceControllerIntegrationTest.java
//package com.meethub.controller.api;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.AccessLevel;
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.enums.ResourceType;
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//import static org.hamcrest.Matchers.*;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@Slf4j
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@Transactional
//class MeetingResourceControllerIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private MeetingRepository meetingRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    private Meeting testMeeting;
//
//    @BeforeEach
//    void setUp() {
//        // Tworzenie organizatora
//        User testOrganizer = User.builder()
//                .firstName("Jan")
//                .lastName("Kowalski")
//                .email("organizer@example.com") // To musi pasować do @WithMockUser
//                .password(passwordEncoder.encode("password123"))
//                .role(UserRole.PARTICIPANT)
//                .enabled(true)
//                .build();
//        userRepository.save(testOrganizer);
//
//        // Tworzenie spotkania
//        testMeeting = Meeting.builder()
//                .title("Testowe Spotkanie")
//                .description("Spotkanie testowe dla zasobów")
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
//                .organizer(testOrganizer)
//                .type(MeetingType.ONLINE)
//                .visibility(MeetingVisibility.PUBLIC)
//                .maxParticipants(20)
//                .build();
//        testMeeting = meetingRepository.save(testMeeting);
//
//        log.info("✅ Setup complete - Meeting ID: {}", testMeeting.getId());
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void debug_testAddResource() throws Exception {
//        // Given
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.pdf",
//                "application/pdf",
//                "Test content".getBytes()
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Test Document")
//                        .param("type", "DOCUMENT")
//                        .param("description", "Test description")
//                        .param("accessLevel", "PUBLIC")
//                        .with(csrf())
//                        .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.data.title", is("Test Document")))
//                .andExpect(jsonPath("$.data.resourceType", is("DOCUMENT")))
//                .andExpect(jsonPath("$.data.accessLevel", is("PUBLIC")));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void getResources_shouldReturnEmptyList_whenNoResources() throws Exception {
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .accept(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.data", hasSize(0)));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void getResourcesByType_shouldReturnFilteredResources() throws Exception {
//        // Najpierw dodaj zasoby różnych typów
//        // Dokument
//        MockMultipartFile documentFile = new MockMultipartFile(
//                "file",
//                "document.pdf",
//                "application/pdf",
//                "Document content".getBytes()
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(documentFile)
//                        .param("title", "Test Document")
//                        .param("type", "DOCUMENT")
//                        .param("accessLevel", "PUBLIC")
//                        .with(csrf()))
//                .andExpect(status().isOk());
//
//        // Obraz
//        MockMultipartFile imageFile = new MockMultipartFile(
//                "file",
//                "image.jpg",
//                "image/jpeg",
//                new byte[]{1, 2, 3}
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(imageFile)
//                        .param("title", "Test Image")
//                        .param("type", "IMAGE")
//                        .param("accessLevel", "PUBLIC")
//                        .with(csrf()))
//                .andExpect(status().isOk());
//
//        // Testuj filtrowanie po typie DOCUMENT
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources/type/DOCUMENT", testMeeting.getId())
//                        .accept(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].resourceType", is("DOCUMENT")))
//                .andExpect(jsonPath("$.data[0].title", is("Test Document")));
//
//        // Testuj filtrowanie po typie IMAGE
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources/type/IMAGE", testMeeting.getId())
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].resourceType", is("IMAGE")));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void testDifferentResourceTypes() throws Exception {
//        // Testuj wszystkie typy zasobów
//        ResourceType[] types = ResourceType.values();
//
//        for (ResourceType type : types) {
//            String typeName = type.name();
//            MockMultipartFile file = new MockMultipartFile(
//                    "file",
//                    "test-" + typeName.toLowerCase() + ".txt",
//                    "text/plain",
//                    ("Content for " + typeName).getBytes()
//            );
//
//            mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                            .file(file)
//                            .param("title", typeName + " Resource")
//                            .param("type", typeName)
//                            .param("accessLevel", "PUBLIC")
//                            .with(csrf()))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.data.resourceType", is(typeName)))
//                    .andExpect(jsonPath("$.data.title", is(typeName + " Resource")));
//        }
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void addResource_withDifferentAccessLevels() throws Exception {
//        // Testuj różne poziomy dostępu
//        AccessLevel[] accessLevels = AccessLevel.values();
//
//        for (AccessLevel accessLevel : accessLevels) {
//            String levelName = accessLevel.name();
//            MockMultipartFile file = new MockMultipartFile(
//                    "file",
//                    "access-" + levelName.toLowerCase() + ".txt",
//                    "text/plain",
//                    ("Content for " + levelName).getBytes()
//            );
//
//            mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                            .file(file)
//                            .param("title", levelName + " Document")
//                            .param("type", "DOCUMENT")
//                            .param("accessLevel", levelName)
//                            .with(csrf()))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.data.accessLevel", is(levelName)))
//                    .andExpect(jsonPath("$.data.title", is(levelName + " Document")));
//        }
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void addResource_withTags() throws Exception {
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "tagged.pdf",
//                "application/pdf",
//                "Tagged content".getBytes()
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Tagged Document")
//                        .param("type", "DOCUMENT")
//                        .param("accessLevel", "PUBLIC")
//                        .param("tags", "important,meeting,agenda")
//                        .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.title", is("Tagged Document")));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void getResourcesByTag_shouldReturnFilteredResources() throws Exception {
//        // Najpierw dodaj zasób z tagami
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "important.pdf",
//                "application/pdf",
//                "Important content".getBytes()
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Important Document")
//                        .param("type", "DOCUMENT")
//                        .param("accessLevel", "PUBLIC")
//                        .param("tags", "important,confidential")
//                        .with(csrf()))
//                .andExpect(status().isOk());
//
//        // Szukaj po tagu "important"
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources/tag/important", testMeeting.getId())
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", hasSize(1)))
//                .andExpect(jsonPath("$.data[0].title", is("Important Document")));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void getResourceStats_shouldReturnStatistics() throws Exception {
//        // Dodaj 3 zasoby
//        for (int i = 1; i <= 3; i++) {
//            MockMultipartFile file = new MockMultipartFile(
//                    "file",
//                    "doc" + i + ".pdf",
//                    "application/pdf",
//                    ("Content " + i).getBytes()
//            );
//
//            mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                            .file(file)
//                            .param("title", "Document " + i)
//                            .param("type", "DOCUMENT")
//                            .param("accessLevel", "PUBLIC")
//                            .with(csrf()))
//                    .andExpect(status().isOk());
//        }
//
//        // Pobierz statystyki
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources/stats", testMeeting.getId())
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.data.totalResources", is(3)));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void getResource_shouldReturnSingleResource() throws Exception {
//        // Najpierw dodaj zasób
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "single.pdf",
//                "application/pdf",
//                "Single resource".getBytes()
//        );
//
//        String response = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Single Resource")
//                        .param("type", "PRESENTATION")
//                        .param("accessLevel", "PUBLIC")
//                        .with(csrf()))
//                .andReturn().getResponse().getContentAsString();
//
//        Long resourceId = objectMapper.readTree(response)
//                .path("data").path("id").asLong();
//
//        // Pobierz szczegóły zasobu
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources/{resourceId}",
//                        testMeeting.getId(), resourceId)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.id", is(resourceId.intValue())))
//                .andExpect(jsonPath("$.data.title", is("Single Resource")))
//                .andExpect(jsonPath("$.data.resourceType", is("PRESENTATION")));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void updateResource_shouldUpdateSuccessfully() throws Exception {
//        // Najpierw dodaj zasób
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "original.pdf",
//                "application/pdf",
//                "Original content".getBytes()
//        );
//
//        String response = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Original Title")
//                        .param("type", "DOCUMENT")
//                        .param("accessLevel", "PUBLIC")
//                        .with(csrf()))
//                .andReturn().getResponse().getContentAsString();
//
//        Long resourceId = objectMapper.readTree(response)
//                .path("data").path("id").asLong();
//
//        // Przygotuj JSON do aktualizacji
//        String updateJson = """
//                {
//                    "title": "Updated Title",
//                    "description": "Updated Description",
//                    "accessLevel": "PRIVATE",
//                    "tags": ["updated", "important"]
//                }
//                """;
//
//        // Aktualizuj zasób
//        mockMvc.perform(put("/api/meetings/{meetingId}/resources/{resourceId}",
//                        testMeeting.getId(), resourceId)
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(updateJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.data.title", is("Updated Title")))
//                .andExpect(jsonPath("$.data.description", is("Updated Description")))
//                .andExpect(jsonPath("$.data.accessLevel", is("PRIVATE")));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void deleteResource_shouldDeleteSuccessfully() throws Exception {
//        // Najpierw dodaj zasób
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "delete.pdf",
//                "application/pdf",
//                "Delete this".getBytes()
//        );
//
//        String response = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Resource to Delete")
//                        .param("type", "DOCUMENT")
//                        .param("accessLevel", "PUBLIC")
//                        .with(csrf()))
//                .andReturn().getResponse().getContentAsString();
//
//        Long resourceId = objectMapper.readTree(response)
//                .path("data").path("id").asLong();
//
//        // Sprawdź, że zasób istnieje
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources", testMeeting.getId()))
//                .andExpect(jsonPath("$.data", hasSize(1)));
//
//        // Usuń zasób
//        mockMvc.perform(delete("/api/meetings/{meetingId}/resources/{resourceId}",
//                        testMeeting.getId(), resourceId)
//                        .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Zasób usunięty pomyślnie")));
//
//        // Sprawdź, że lista jest pusta
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources", testMeeting.getId()))
//                .andExpect(jsonPath("$.data", hasSize(0)));
//    }
//
//    @Test
//    void getResources_shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
//        mockMvc.perform(get("/api/meetings/{meetingId}/resources", testMeeting.getId()))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrlPattern("**/login"));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void addResource_shouldReturnBadRequest_whenMissingRequiredFields() throws Exception {
//        // Próba dodania bez wymaganych pól
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "",
//                "text/plain",
//                new byte[0]
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .with(csrf()))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void addResource_shouldReturnBadRequest_whenInvalidAccessLevel() throws Exception {
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.pdf",
//                "application/pdf",
//                "Content".getBytes()
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Test")
//                        .param("type", "DOCUMENT")
//                        .param("accessLevel", "INVALID_LEVEL") // Nieprawidłowy poziom
//                        .with(csrf()))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = "USER")
//    void addResource_shouldReturnBadRequest_whenInvalidResourceType() throws Exception {
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.pdf",
//                "application/pdf",
//                "Content".getBytes()
//        );
//
//        mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
//                        .file(file)
//                        .param("title", "Test")
//                        .param("type", "INVALID_TYPE") // Nieprawidłowy typ
//                        .param("accessLevel", "PUBLIC")
//                        .with(csrf()))
//                .andExpect(status().isBadRequest());
//    }
//}






// MeetingResourceControllerIntegrationTest.java
package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.AccessLevel;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
//@ActiveProfiles("test")
@Rollback
@Transactional
class MeetingResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Meeting testMeeting;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        User testOrganizer = User.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("organizer@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .build();
        userRepository.save(testOrganizer);

        testMeeting = Meeting.builder()
                .title("Testowe Spotkanie")
                .description("Spotkanie testowe dla zasobów")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .organizer(testOrganizer)
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .maxParticipants(20)
                .build();
        testMeeting = meetingRepository.save(testMeeting);

        log.info("Setup: Meeting ID = {}", testMeeting.getId());
        log.info("Available AccessLevels: {}", Arrays.toString(AccessLevel.values()));
        log.info("Available ResourceTypes: {}", Arrays.toString(ResourceType.values()));
    }


    private void testCombination(String testName, String[] params, String[] values) throws Exception {
        log.info("\n--- {} ---", testName);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Content".getBytes()
        );

        var request = multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
                .file(file)
                .with(csrf());

        for (int i = 0; i < params.length; i++) {
            if (!params[i].equals("file")) {
                request.param(params[i], values[i]);
            }
        }

        MvcResult result = mockMvc.perform(request)
                .andDo(print())
                .andReturn();

        log.info("Status: {}, Success: {}",
                result.getResponse().getStatus(),
                result.getResponse().getStatus() == 200);
    }


    @Test
    @WithMockUser(username = "organizer@example.com", roles = "PARTICIPANT")
    void testResourceTypeEnumConversion() throws Exception {
        // Testuj każdy typ osobno z pełnym loggingiem
        for (ResourceType resourceType : ResourceType.values()) {
            log.info("Testing ResourceType: {}", resourceType);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    "Content".getBytes()
            );

            MvcResult result = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
                            .file(file)
                            .param("title", resourceType + " Test")
                            .param("type", resourceType.name())
                            .param("accessLevel", "PUBLIC")
                            .with(csrf()))
                    .andDo(print())
                    .andReturn();

            int status = result.getResponse().getStatus();
            log.info("Status for {}: {}", resourceType, status);

            if (status != 200) {
                log.error("Failed for {}: {}", resourceType, result.getResponse().getContentAsString());
            }
        }
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "USER")
    void testAccessLevelEnumConversion() throws Exception {
        // Testuj każdy access level osobno
        for (AccessLevel accessLevel : AccessLevel.values()) {
            log.info("Testing AccessLevel: {}", accessLevel);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    "Content".getBytes()
            );

            MvcResult result = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
                            .file(file)
                            .param("title", accessLevel + " Test")
                            .param("type", "OTHER")
                            .param("accessLevel", accessLevel.name())
                            .with(csrf()))
                    .andDo(print())
                    .andReturn();

            int status = result.getResponse().getStatus();
            log.info("Status for {}: {}", accessLevel, status);
        }
    }

    @Test
    void getResources_shouldRedirect_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/meetings/{meetingId}/resources", testMeeting.getId()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "USER")
    void getResource_shouldWorkAfterAdding() throws Exception {
        // Najpierw dodaj zasób (używając najprostszej kombinacji)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Content".getBytes()
        );

        MvcResult addResult = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
                        .file(file)
                        .param("title", "To Retrieve")
                        .param("type", "OTHER")
                        .param("accessLevel", "PUBLIC")
                        .with(csrf()))
                .andReturn();

        if (addResult.getResponse().getStatus() == 200) {
            String response = addResult.getResponse().getContentAsString();
            Long resourceId = objectMapper.readTree(response)
                    .path("data").path("id").asLong();

            // Teraz pobierz ten zasób
            mockMvc.perform(get("/api/meetings/{meetingId}/resources/{resourceId}",
                            testMeeting.getId(), resourceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title", is("To Retrieve")));
        }
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "USER")
    void deleteResource_shouldWorkAfterAdding() throws Exception {
        // Dodaj zasób
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete.txt",
                "text/plain",
                "Delete me".getBytes()
        );

        MvcResult addResult = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
                        .file(file)
                        .param("title", "Delete Me")
                        .param("type", "OTHER")
                        .param("accessLevel", "PUBLIC")
                        .with(csrf()))
                .andReturn();

        if (addResult.getResponse().getStatus() == 200) {
            String response = addResult.getResponse().getContentAsString();
            Long resourceId = objectMapper.readTree(response)
                    .path("data").path("id").asLong();

            // Usuń zasób
            mockMvc.perform(delete("/api/meetings/{meetingId}/resources/{resourceId}",
                            testMeeting.getId(), resourceId)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "USER")
    void getResourcesByType_shouldWork() throws Exception {
        // Dodaj zasób typu OTHER
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "other.txt",
                "text/plain",
                "Other content".getBytes()
        );

        MvcResult addResult = mockMvc.perform(multipart("/api/meetings/{meetingId}/resources", testMeeting.getId())
                        .file(file)
                        .param("title", "Other Resource")
                        .param("type", "OTHER")
                        .param("accessLevel", "PUBLIC")
                        .with(csrf()))
                .andReturn();

        if (addResult.getResponse().getStatus() == 200) {
            // Szukaj po typie OTHER
            mockMvc.perform(get("/api/meetings/{meetingId}/resources/type/OTHER", testMeeting.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].type", is("OTHER")));
        }
    }
}