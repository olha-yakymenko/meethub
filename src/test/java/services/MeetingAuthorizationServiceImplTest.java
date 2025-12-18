package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingResource;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.enums.ResourceAccessLevel;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingResourceRepository;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingAuthorizationServiceImplTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantService meetingParticipantService;

    @Mock
    private MeetingResourceRepository meetingResourceRepository;

    @InjectMocks
    private MeetingAuthorizationServiceImpl meetingAuthorizationService;

    private Meeting publicMeeting;
    private Meeting privateMeeting;
    private User organizer;
    private User participant;
    private User moderator;
    private User contributor;
    private User externalUser;
    private MeetingResource testResource;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(1L)
                .email("organizer@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        participant = User.builder()
                .id(2L)
                .email("participant@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        moderator = User.builder()
                .id(3L)
                .email("moderator@example.com")
                .firstName("Mod")
                .lastName("Erator")
                .build();

        contributor = User.builder()
                .id(4L)
                .email("contributor@example.com")
                .firstName("Contrib")
                .lastName("Utor")
                .build();

        externalUser = User.builder()
                .id(5L)
                .email("external@example.com")
                .firstName("External")
                .lastName("User")
                .build();

        publicMeeting = Meeting.builder()
                .title("Public Meeting")
                .description("Public description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .visibility(MeetingVisibility.PUBLIC)
                .maxParticipants(10)
                .organizer(organizer)
                .build();
        publicMeeting.setId(100L);

        privateMeeting = Meeting.builder()
                .title("Private Meeting")
                .description("Private description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .visibility(MeetingVisibility.PRIVATE)
                .maxParticipants(5)
                .organizer(organizer)
                .build();
        privateMeeting.setId(200L);

        testResource = MeetingResource.builder()
                .filePath("/uploads/test.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .uploadedBy(participant)
                .build();
        testResource.setId(500L);
        testResource.setMeeting(publicMeeting);
    }

    @Test
    void getUserMeetingPermissions_whenOrganizer_shouldReturnFullPermissions() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(100L, 1L);

        assertAll(
                () -> assertTrue(info.isOrganizer()),
                () -> assertTrue(info.isParticipant()),
                () -> assertTrue(info.isCanEdit()),
                () -> assertTrue(info.isCanDelete()),
                () -> assertTrue(info.isCanManageParticipants()),
                () -> assertTrue(info.isCanJoin()),
                () -> assertTrue(info.isCanViewDetails()),
                () -> assertTrue(info.isCanUpload()),
                () -> assertTrue(info.isCanDownload()),
                () -> assertEquals("ORGANIZER", info.getParticipantRole())
        );
    }

    @Test
    void getUserMeetingPermissions_whenPublicMeetingExternalUser_shouldReturnViewOnly() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(100L, 5L);

        assertAll(
                () -> assertFalse(info.isOrganizer()),
                () -> assertFalse(info.isParticipant()),
                () -> assertFalse(info.isCanEdit()),
                () -> assertFalse(info.isCanDelete()),
                () -> assertFalse(info.isCanManageParticipants()),
                () -> assertFalse(info.isCanJoin()),
                () -> assertTrue(info.isCanViewDetails()),
                () -> assertFalse(info.isCanUpload()),
                () -> assertFalse(info.isCanDownload()),
                () -> assertEquals("NONE", info.getParticipantRole())
        );
    }

    @Test
    void getUserMeetingPermissions_whenPrivateMeetingExternalUser_shouldReturnNoAccess() {
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));

        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(200L, 5L);

        assertAll(
                () -> assertFalse(info.isOrganizer()),
                () -> assertFalse(info.isParticipant()),
                () -> assertFalse(info.isCanEdit()),
                () -> assertFalse(info.isCanDelete()),
                () -> assertFalse(info.isCanManageParticipants()),
                () -> assertFalse(info.isCanJoin()),
                () -> assertFalse(info.isCanViewDetails()),
                () -> assertFalse(info.isCanUpload()),
                () -> assertFalse(info.isCanDownload()),
                () -> assertEquals("NONE", info.getParticipantRole())
        );
    }

    @Test
    void getUserMeetingPermissions_whenNullUserId_shouldHandleGracefully() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(100L, null);

        assertAll(
                () -> assertFalse(info.isOrganizer()),
                () -> assertFalse(info.isParticipant()),
                () -> assertTrue(info.isCanViewDetails()),
                () -> assertEquals("NONE", info.getParticipantRole())
        );
    }

    @Test
    void getUserMeetingPermissions_whenMeetingNotFound_shouldThrowException() {
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingAuthorizationService.getUserMeetingPermissions(999L, 1L)
        );
    }

    @Test
    void canUserViewResource_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canView = meetingAuthorizationService.canUserViewResource(100L, 1L);

        assertAll(
                () -> assertTrue(canView)
        );
    }

    @Test
    void canUserViewResource_whenPublicMeetingExternalUser_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        boolean canView = meetingAuthorizationService.canUserViewResource(100L, 5L);

        assertAll(
                () -> assertTrue(canView)
        );
    }

    @Test
    void canUserViewResource_whenPrivateMeetingExternalUser_shouldReturnFalse() {
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));

        boolean canView = meetingAuthorizationService.canUserViewResource(200L, 5L);

        assertAll(
                () -> assertFalse(canView)
        );
    }

    @Test
    void canUserDownloadResource_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canDownload = meetingAuthorizationService.canUserDownloadResource(100L, 1L);

        assertAll(
                () -> assertTrue(canDownload)
        );
    }

    @Test
    void canUserDownloadResource_whenExternalUser_shouldReturnFalse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        boolean canDownload = meetingAuthorizationService.canUserDownloadResource(100L, 5L);

        assertAll(
                () -> assertFalse(canDownload)
        );
    }

    @Test
    void canUserUploadResource_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canUpload = meetingAuthorizationService.canUserUploadResource(100L, 1L);

        assertAll(
                () -> assertTrue(canUpload)
        );
    }

    @Test
    void canUserDeleteResource_whenResourceOwner_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingResourceRepository.findById(500L)).thenReturn(Optional.of(testResource));

        boolean canDelete = meetingAuthorizationService.canUserDeleteResource(100L, 500L, 2L);

        assertAll(
                () -> assertTrue(canDelete)
        );
    }

    @Test
    void canUserDeleteResource_whenNotOwnerOrOrganizer_shouldReturnFalse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingResourceRepository.findById(500L)).thenReturn(Optional.of(testResource));

        boolean canDelete = meetingAuthorizationService.canUserDeleteResource(100L, 500L, 5L);

        assertAll(
                () -> assertFalse(canDelete)
        );
    }

    @Test
    void canUserDeleteResource_whenResourceNotFound_shouldReturnFalse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingResourceRepository.findById(999L)).thenReturn(Optional.empty());

        boolean canDelete = meetingAuthorizationService.canUserDeleteResource(100L, 999L, 2L);

        assertAll(
                () -> assertFalse(canDelete)
        );
    }

    @Test
    void getUserResourceAccessLevel_whenOrganizer_shouldReturnManage() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        ResourceAccessLevel level = meetingAuthorizationService.getUserResourceAccessLevel(100L, 1L);

        assertAll(
                () -> assertEquals(ResourceAccessLevel.MANAGE, level)
        );
    }

    @Test
    void getUserResourceAccessLevel_whenPublicMeetingExternalUser_shouldReturnView() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        ResourceAccessLevel level = meetingAuthorizationService.getUserResourceAccessLevel(100L, 5L);

        assertAll(
                () -> assertEquals(ResourceAccessLevel.VIEW, level)
        );
    }

    @Test
    void getUserResourceAccessLevel_whenPrivateMeetingExternalUser_shouldReturnNone() {
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));

        ResourceAccessLevel level = meetingAuthorizationService.getUserResourceAccessLevel(200L, 5L);

        assertAll(
                () -> assertEquals(ResourceAccessLevel.NONE, level)
        );
    }

    @Test
    void hasResourceAccess_whenSufficientLevel_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean hasAccess = meetingAuthorizationService.hasResourceAccess(100L, 1L, ResourceAccessLevel.UPLOAD);

        assertAll(
                () -> assertTrue(hasAccess)
        );
    }

    @Test
    void canUserComment_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canComment = meetingAuthorizationService.canUserComment(100L, 1L);

        assertAll(
                () -> assertTrue(canComment)
        );
    }

    @Test
    void canUserComment_whenExternalUser_shouldReturnFalse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        boolean canComment = meetingAuthorizationService.canUserComment(100L, 5L);

        assertAll(
                () -> assertFalse(canComment)
        );
    }

    @Test
    void canUserViewParticipants_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canView = meetingAuthorizationService.canUserViewParticipants(100L, 1L);

        assertAll(
                () -> assertTrue(canView)
        );
    }

    @Test
    void canUserViewParticipants_whenPublicMeetingExternalUser_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        boolean canView = meetingAuthorizationService.canUserViewParticipants(100L, 5L);

        assertAll(
                () -> assertTrue(canView)
        );
    }

    @Test
    void canUserViewParticipants_whenPrivateMeetingExternalUser_shouldReturnFalse() {
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));

        boolean canView = meetingAuthorizationService.canUserViewParticipants(200L, 5L);

        assertAll(
                () -> assertFalse(canView)
        );
    }

    @Test
    void canUserEditMeeting_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canEdit = meetingAuthorizationService.canUserEditMeeting(100L, 1L);

        assertAll(
                () -> assertTrue(canEdit)
        );
    }

    @Test
    void canUserDeleteMeeting_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canDelete = meetingAuthorizationService.canUserDeleteMeeting(100L, 1L);

        assertAll(
                () -> assertTrue(canDelete)
        );
    }

    @Test
    void canUserManageParticipants_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        boolean canManage = meetingAuthorizationService.canUserManageParticipants(100L, 1L);

        assertAll(
                () -> assertTrue(canManage)
        );
    }
}