package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import security.WithCustomUser;
import security.WithUserId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.meethub.domain.model.enums.MeetingStatus.CANCELLED;
import static com.meethub.domain.model.enums.MeetingType.ONLINE;
import static com.meethub.domain.model.enums.MeetingVisibility.PUBLIC;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private MeetingResponse mockMeeting;

    @BeforeEach
    void setUp() {
        mockMeeting = new MeetingResponse();
        mockMeeting.setId(1L);
        mockMeeting.setTitle("Test Meeting");
        mockMeeting.setDescription("Test Description");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMeeting_ShouldReturnMeetingDetails() throws Exception {
        when(meetingService.getMeetingById(1L)).thenReturn(mockMeeting);

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Szczegóły spotkania pobrane pomyślnie"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Meeting"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMeeting_ShouldReturnBadRequest_WhenInvalidMeetingId() throws Exception {
        mockMvc.perform(get("/api/v1/meetings/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }


    @Test
    void getUserMeetings_ShouldReturnUnauthorized_WhenUserNotLoggedIn() throws Exception {
        // When & Then - brak autoryzacji
        mockMvc.perform(get("/api/v1/meetings/my-meetings")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized()); // 401 zamiast 400
    }


    @ParameterizedTest
    @MethodSource("provideInvalidLocationParameters")
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void findNearbyMeetings_ShouldValidateParameters(
            Double latitude, Double longitude, Double radius, int expectedStatus) throws Exception {
        // When & Then
        var request = get("/api/v1/meetings/nearby");

        if (latitude != null) {
            request.param("latitude", latitude.toString());
        }
        if (longitude != null) {
            request.param("longitude", longitude.toString());
        }
        if (radius != null) {
            request.param("radius", radius.toString());
        }

        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus));
    }

    private static Stream<Arguments> provideInvalidLocationParameters() {
        return Stream.of(
                // latitude, longitude, radius, expectedStatus
                Arguments.of(91.0, 21.0122, 5000.0, 400), // Nieprawidłowa latitude
                Arguments.of(52.2297, 181.0, 5000.0, 400), // Nieprawidłowa longitude
                Arguments.of(52.2297, 21.0122, 50.0, 400), // Promień za mały
                Arguments.of(52.2297, 21.0122, 200000.0, 400) // Promień za duży
        );
    }

//    @Test
//    @WithMockUser(username = "test@example.com", roles = {"USER"})
//    void deleteMeeting_ShouldReturnSuccess_WhenAuthorized() throws Exception {
//        // When & Then
//        mockMvc.perform(delete("/api/v1/meetings/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Spotkanie usunięte pomyślnie"));
//
//        verify(meetingService).deleteMeeting(1L, 1L);
//    }
//
//    @Test
//    void deleteMeeting_ShouldReturnUnauthorized_WhenUserNotLoggedIn() throws Exception {
//        // When & Then
//        mockMvc.perform(delete("/api/v1/meetings/1"))
//                .andExpect(status().isUnauthorized()); // 401 zamiast 400
//    }
//
//    @Test
//    @WithMockUser(username = "test@example.com", roles = {"USER"})
//    void updateMeeting_ShouldUpdateAndReturnMeeting() throws Exception {
//        // Given
//        UpdateMeetingRequest request = new UpdateMeetingRequest();
//        request.setTitle("Updated Title");
//        request.setDescription("Updated Description");
//
//        MeetingResponse updatedMeeting = new MeetingResponse();
//        updatedMeeting.setId(1L);
//        updatedMeeting.setTitle("Updated Title");
//
//        when(meetingService.updateMeeting(eq(1L), any(UpdateMeetingRequest.class), eq(1L)))
//                .thenReturn(updatedMeeting);
//
//        // When & Then
//        mockMvc.perform(put("/api/v1/meetings/1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Spotkanie zaktualizowane pomyślnie"))
//                .andExpect(jsonPath("$.data.title").value("Updated Title"));
//    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getUpcomingPublicMeetings_ShouldWorkWithAuthentication() throws Exception {
        // Given
        List<MeetingResponse> meetings = Arrays.asList(mockMeeting);
        when(meetingService.getUpcomingPublicMeetings()).thenReturn(meetings);

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/public/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nadchodzące spotkania publiczne pobrane pomyślnie"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void searchMeetings_ShouldReturnFilteredResults() throws Exception {
        // Given
        Page<MeetingResponse> page = new PageImpl<>(
                Arrays.asList(mockMeeting),
                PageRequest.of(0, 10),
                1
        );

        when(meetingService.getFilteredMeetings(anyString(), anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/search")
                        .param("query", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Wyniki wyszukiwania pobrane pomyślnie"));
    }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void createMeeting_ShouldCreateMeeting() throws Exception {
            // Given
            CreateMeetingRequest request = CreateMeetingRequest.builder()
                    .title("New Meeting")
                    .description("Meeting Description")
                    .startDate(LocalDateTime.now().plusDays(1))
                    .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                    .type(ONLINE)
                    .visibility(PUBLIC)
                    .build();

            MeetingResponse createdMeeting = new MeetingResponse();
            createdMeeting.setId(2L);
            createdMeeting.setTitle("New Meeting");
            createdMeeting.setDescription("Meeting Description");

            when(meetingService.createMeeting(any(CreateMeetingRequest.class), eq(1L)))
                    .thenReturn(createdMeeting);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Spotkanie utworzone pomyślnie"))
                    .andExpect(jsonPath("$.data.title").value("New Meeting"));

            verify(meetingService).createMeeting(any(CreateMeetingRequest.class), eq(1L));
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void createMeeting_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
            // Given - niepoprawne dane (brak tytułu)
            CreateMeetingRequest invalidRequest = CreateMeetingRequest.builder()
                    .description("Meeting without title")
                    .startDate(LocalDateTime.now().plusDays(1))
                    .endDate(LocalDateTime.now())
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void updateMeeting_ShouldUpdateAndReturnMeeting() throws Exception {
            // Given
            UpdateMeetingRequest request = new UpdateMeetingRequest();
            request.setTitle("Updated Title");
            request.setDescription("Updated Description");

            MeetingResponse updatedMeeting = new MeetingResponse();
            updatedMeeting.setId(1L);
            updatedMeeting.setTitle("Updated Title");

            when(meetingService.updateMeeting(eq(1L), any(UpdateMeetingRequest.class), eq(1L)))
                    .thenReturn(updatedMeeting);

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Spotkanie zaktualizowane pomyślnie"))
                    .andExpect(jsonPath("$.data.title").value("Updated Title"));

            verify(meetingService).updateMeeting(eq(1L), any(UpdateMeetingRequest.class), eq(1L));
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void updateMeeting_ShouldReturnBadRequest_WhenInvalidMeetingId() throws Exception {
            // Given
            UpdateMeetingRequest request = new UpdateMeetingRequest();
            request.setTitle("Updated Title");

            // When & Then
            mockMvc.perform(put("/api/v1/meetings/0")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void deleteMeeting_ShouldDeleteMeeting() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/v1/meetings/1").with(csrf()))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Spotkanie usunięte pomyślnie"));

            verify(meetingService).deleteMeeting(1L, 1L);
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void deleteMeeting_ShouldReturnBadRequest_WhenInvalidMeetingId() throws Exception {
            mockMvc.perform(delete("/api/v1/meetings/0").with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void getUserMeetings_ShouldReturnUserMeetings() throws Exception {
            // Given
            Page<MeetingResponse> page = new PageImpl<>(
                    Arrays.asList(mockMeeting),
                    PageRequest.of(0, 10),
                    1
            );

            when(meetingService.getUserMeetings(eq(1L), any(PageRequest.class)))
                    .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/my-meetings")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Spotkania użytkownika pobrane pomyślnie"))
                    .andExpect(jsonPath("$.data.content[0].id").value(1));
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void findNearbyMeetings_ShouldReturnNearbyMeetings() throws Exception {
            // Given
            NearbyMeetingsRequest request = new NearbyMeetingsRequest();
            request.setLatitude(52.2297);
            request.setLongitude(21.0122);
            request.setRadius(5000.0);

            List<MeetingResponse> meetings = Arrays.asList(mockMeeting);
            when(meetingService.findNearbyMeetings(52.2297, 21.0122, 5000.0))
                    .thenReturn(meetings);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings/nearby")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Spotkania w pobliżu znalezione pomyślnie"))
                    .andExpect(jsonPath("$.data").isArray());
        }

    @Test
    @WithUserId(value = 1L, roles = {"ORGANIZER"})
    void changeMeetingStatus_ShouldChangeStatus_WhenUserIsOrganizer() throws Exception {
        // Given
        ChangeMeetingStatusRequest request = new ChangeMeetingStatusRequest();
        request.setStatus(CANCELLED);

        doNothing().when(meetingService).changeMeetingStatus(1L, CANCELLED, 1L);

        // When & Then
        mockMvc.perform(patch("/api/v1/meetings/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Status spotkania zmieniony pomyślnie"));

        verify(meetingService).changeMeetingStatus(1L, CANCELLED, 1L);
    }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void duplicateMeeting_ShouldDuplicateMeeting() throws Exception {
            // Given
            MeetingResponse duplicate = new MeetingResponse();
            duplicate.setId(2L);
            duplicate.setTitle("Test Meeting - Copy");

            when(meetingService.duplicateMeeting(1L, 1L)).thenReturn(duplicate);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings/1/duplicate").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Spotanie zduplikowane pomyślnie"))
                    .andExpect(jsonPath("$.data.title").value("Test Meeting - Copy"));

            verify(meetingService).duplicateMeeting(1L, 1L);
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void duplicateMeeting_ShouldReturnBadRequest_WhenInvalidMeetingId() throws Exception {
            mockMvc.perform(post("/api/v1/meetings/0/duplicate").with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void getMyTemplates_ShouldReturnTemplates() throws Exception {
            // Given
            List<MeetingResponse> templates = Arrays.asList(mockMeeting);
            when(meetingService.getMeetingTemplates(1L)).thenReturn(templates);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/templates/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Szablony użytkownika pobrane pomyślnie"));
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void createFromTemplate_ShouldCreateMeetingFromTemplate() throws Exception {
            // Given
            CreateFromTemplateRequest request = new CreateFromTemplateRequest();
            request.setNewStartDate(LocalDateTime.now().plusDays(3));

            MeetingResponse newMeeting = new MeetingResponse();
            newMeeting.setId(3L);
            newMeeting.setTitle("Meeting from Template");

            when(meetingService.createFromTemplate(eq(1L), eq(1L), any(LocalDateTime.class)))
                    .thenReturn(newMeeting);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings/templates/1/create")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Spotkanie utworzone z szablonu pomyślnie"))
                    .andExpect(jsonPath("$.data.title").value("Meeting from Template"));
        }

//        @Test
//        @WithUserId(value = 1L, roles = {"ORGANIZER"})
//        void createFromTemplate_ShouldReturnBadRequest_WhenInvalidTemplateId() throws Exception {
//            // Given
//            CreateFromTemplateRequest request = new CreateFromTemplateRequest();
//            request.setNewStartDate(LocalDateTime.now().plusDays(3));
//
//
//            // When & Then
//            mockMvc.perform(post("/api/v1/meetings/templates/0/create")
//                            .with(csrf())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isBadRequest());
//        }


        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void searchMeetings_WithAllParameters_ShouldReturnResults() throws Exception {
            // Given
            Page<MeetingResponse> page = new PageImpl<>(
                    Arrays.asList(mockMeeting),
                    PageRequest.of(0, 10),
                    1
            );

            when(meetingService.getFilteredMeetings(eq("test"), eq("ONLINE"), eq("SCHEDULED"), any(PageRequest.class)))
                    .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/search")
                            .param("query", "test")
                            .param("type", "ONLINE")
                            .param("status", "SCHEDULED")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void searchMeetings_WithoutParameters_ShouldReturnResults() throws Exception {
            // Given
            Page<MeetingResponse> page = new PageImpl<>(
                    Arrays.asList(mockMeeting),
                    PageRequest.of(0, 10),
                    1
            );

            when(meetingService.getFilteredMeetings(isNull(), isNull(), isNull(), any(PageRequest.class)))
                    .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/search")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void getMeeting_ShouldReturnNotFound_WhenMeetingDoesNotExist() throws Exception {
            // Given
            when(meetingService.getMeetingById(999L))
                    .thenThrow(new com.meethub.exception.ResourceNotFoundException("Meeting not found"));

            // When & Then
            mockMvc.perform(get("/api/v1/meetings/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void createMeeting_WithInvalidDates_ShouldReturnBadRequest() throws Exception {
            // Given - endDate przed startDate
            CreateMeetingRequest invalidRequest = CreateMeetingRequest.builder()
                    .title("Invalid Meeting")
                    .description("Meeting with invalid dates")
                    .startDate(LocalDateTime.now().plusDays(2))
                    .endDate(LocalDateTime.now().plusDays(1)) // endDate przed startDate
                    .type(ONLINE)
                    .visibility(PUBLIC)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void createMeeting_WithCustomUser_ShouldCreateMeeting() throws Exception {
            // Given
            CreateMeetingRequest request = CreateMeetingRequest.builder()
                    .title("Custom User Meeting")
                    .description("Meeting created by custom user")
                    .startDate(LocalDateTime.now().plusDays(1))
                    .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                    .type(ONLINE)
                    .visibility(PUBLIC)
                    .build();

            MeetingResponse createdMeeting = new MeetingResponse();
            createdMeeting.setId(10L);
            createdMeeting.setTitle("Custom User Meeting");

            when(meetingService.createMeeting(any(CreateMeetingRequest.class), eq(1L)))
                    .thenReturn(createdMeeting);

            // When & Then
            mockMvc.perform(post("/api/v1/meetings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("Custom User Meeting"));
        }


        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void searchMeetings_WithInvalidPageable_ShouldReturnBadRequest() throws Exception {
            // When & Then - nieprawidłowe parametry paginacji
            mockMvc.perform(get("/api/v1/meetings/search")
                            .param("page", "-1") // Ujemna strona
                            .param("size", "0")) // Rozmiar 0
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserId(value = 1L, roles = {"ORGANIZER"})
        void getUserMeetings_WithInvalidPageable_ShouldReturnBadRequest() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/meetings/my-meetings")
                            .param("page", "abc") // Nieprawidłowy format
                            .param("size", "10"))
                    .andExpect(status().isBadRequest());
        }
    }

