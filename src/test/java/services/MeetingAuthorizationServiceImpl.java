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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
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
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(100L, 1L);

        // Then
        assertThat(info.isOrganizer()).isTrue();
        assertThat(info.isParticipant()).isTrue();
        assertThat(info.isCanEdit()).isTrue();
        assertThat(info.isCanDelete()).isTrue();
        assertThat(info.isCanManageParticipants()).isTrue();
        assertThat(info.isCanJoin()).isTrue();
        assertThat(info.isCanViewDetails()).isTrue();
        assertThat(info.isCanUpload()).isTrue();
        assertThat(info.isCanDownload()).isTrue();
        assertThat(info.getParticipantRole()).isEqualTo("ORGANIZER");
    }



    @Test
    void getUserMeetingPermissions_whenPublicMeetingExternalUser_shouldReturnViewOnly() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        // Nie trzeba mockować dla external user - domyślnie zwróci false

        // When
        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(100L, 5L);

        // Then
        assertThat(info.isOrganizer()).isFalse();
        assertThat(info.isParticipant()).isFalse();
        assertThat(info.isCanEdit()).isFalse();
        assertThat(info.isCanDelete()).isFalse();
        assertThat(info.isCanManageParticipants()).isFalse();
        assertThat(info.isCanJoin()).isFalse();
        assertThat(info.isCanViewDetails()).isTrue(); // Public meeting - widzi szczegóły
        assertThat(info.isCanUpload()).isFalse();
        assertThat(info.isCanDownload()).isFalse();
        assertThat(info.getParticipantRole()).isEqualTo("NONE");
    }

    @Test
    void getUserMeetingPermissions_whenPrivateMeetingExternalUser_shouldReturnNoAccess() {
        // Given
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));
        // Nie trzeba mockować dla external user - domyślnie zwróci false

        // When
        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(200L, 5L);

        // Then
        assertThat(info.isOrganizer()).isFalse();
        assertThat(info.isParticipant()).isFalse();
        assertThat(info.isCanEdit()).isFalse();
        assertThat(info.isCanDelete()).isFalse();
        assertThat(info.isCanManageParticipants()).isFalse();
        assertThat(info.isCanJoin()).isFalse();
        assertThat(info.isCanViewDetails()).isFalse(); // Private meeting - nie widzi szczegółów
        assertThat(info.isCanUpload()).isFalse();
        assertThat(info.isCanDownload()).isFalse();
        assertThat(info.getParticipantRole()).isEqualTo("NONE");
    }

    @Test
    void getUserMeetingPermissions_whenNullUserId_shouldHandleGracefully() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));

        // When
        MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(100L, null);

        // Then
        assertThat(info.isOrganizer()).isFalse();
        assertThat(info.isParticipant()).isFalse();
        assertThat(info.isCanViewDetails()).isTrue(); // Public meeting dla niezalogowanych
        assertThat(info.getParticipantRole()).isEqualTo("NONE");
    }

    @Test
    void getUserMeetingPermissions_whenMeetingNotFound_shouldThrowException() {
        // Given
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingAuthorizationService.getUserMeetingPermissions(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Meeting not found");
    }

    @Test
    void canUserViewResource_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canView = meetingAuthorizationService.canUserViewResource(100L, 1L);

        // Then
        assertThat(canView).isTrue();
    }


    @Test
    void canUserViewResource_whenPublicMeetingExternalUser_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        // Nie trzeba mockować dla external user

        // When
        boolean canView = meetingAuthorizationService.canUserViewResource(100L, 5L);

        // Then
        assertThat(canView).isTrue(); // Public meeting - widzi zasoby
    }

    @Test
    void canUserViewResource_whenPrivateMeetingExternalUser_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));
        // Nie trzeba mockować dla external user

        // When
        boolean canView = meetingAuthorizationService.canUserViewResource(200L, 5L);

        // Then
        assertThat(canView).isFalse(); // Private meeting - nie widzi zasobów
    }

    @Test
    void canUserDownloadResource_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canDownload = meetingAuthorizationService.canUserDownloadResource(100L, 1L);

        // Then
        assertThat(canDownload).isTrue();
    }


    @Test
    void canUserDownloadResource_whenExternalUser_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        // Nie trzeba mockować dla external user

        // When
        boolean canDownload = meetingAuthorizationService.canUserDownloadResource(100L, 5L);

        // Then
        assertThat(canDownload).isFalse();
    }

    @Test
    void canUserUploadResource_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canUpload = meetingAuthorizationService.canUserUploadResource(100L, 1L);

        // Then
        assertThat(canUpload).isTrue();
    }


    @Test
    void canUserDeleteResource_whenResourceOwner_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingResourceRepository.findById(500L)).thenReturn(Optional.of(testResource));

        // When
        boolean canDelete = meetingAuthorizationService.canUserDeleteResource(100L, 500L, 2L);

        // Then
        assertThat(canDelete).isTrue(); // Właściciel zasobu może go usunąć
    }

    @Test
    void canUserDeleteResource_whenNotOwnerOrOrganizer_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingResourceRepository.findById(500L)).thenReturn(Optional.of(testResource));

        // When
        boolean canDelete = meetingAuthorizationService.canUserDeleteResource(100L, 500L, 5L);

        // Then
        assertThat(canDelete).isFalse();
    }

    @Test
    void canUserDeleteResource_whenResourceNotFound_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingResourceRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean canDelete = meetingAuthorizationService.canUserDeleteResource(100L, 999L, 2L);

        // Then
        assertThat(canDelete).isFalse();
    }

    @Test
    void getUserResourceAccessLevel_whenOrganizer_shouldReturnManage() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        ResourceAccessLevel level = meetingAuthorizationService.getUserResourceAccessLevel(100L, 1L);

        // Then
        assertThat(level).isEqualTo(ResourceAccessLevel.MANAGE);
    }


    @Test
    void getUserResourceAccessLevel_whenPublicMeetingExternalUser_shouldReturnView() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        // Nie trzeba mockować dla external user

        // When
        ResourceAccessLevel level = meetingAuthorizationService.getUserResourceAccessLevel(100L, 5L);

        // Then
        assertThat(level).isEqualTo(ResourceAccessLevel.VIEW);
    }

    @Test
    void getUserResourceAccessLevel_whenPrivateMeetingExternalUser_shouldReturnNone() {
        // Given
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));
        // Nie trzeba mockować dla external user

        // When
        ResourceAccessLevel level = meetingAuthorizationService.getUserResourceAccessLevel(200L, 5L);

        // Then
        assertThat(level).isEqualTo(ResourceAccessLevel.NONE);
    }

    @Test
    void hasResourceAccess_whenSufficientLevel_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean hasAccess = meetingAuthorizationService.hasResourceAccess(100L, 1L, ResourceAccessLevel.UPLOAD);

        // Then
        assertThat(hasAccess).isTrue();
    }



    @Test
    void canUserComment_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canComment = meetingAuthorizationService.canUserComment(100L, 1L);

        // Then
        assertThat(canComment).isTrue();
    }



    @Test
    void canUserComment_whenExternalUser_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        // Nie trzeba mockować dla external user

        // When
        boolean canComment = meetingAuthorizationService.canUserComment(100L, 5L);

        // Then
        assertThat(canComment).isFalse();
    }

    @Test
    void canUserViewParticipants_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canView = meetingAuthorizationService.canUserViewParticipants(100L, 1L);

        // Then
        assertThat(canView).isTrue();
    }



    @Test
    void canUserViewParticipants_whenPublicMeetingExternalUser_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        // Nie trzeba mockować dla external user

        // When
        boolean canView = meetingAuthorizationService.canUserViewParticipants(100L, 5L);

        // Then
        assertThat(canView).isTrue(); // Public meeting - widzi uczestników
    }

    @Test
    void canUserViewParticipants_whenPrivateMeetingExternalUser_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(privateMeeting));
        // Nie trzeba mockować dla external user

        // When
        boolean canView = meetingAuthorizationService.canUserViewParticipants(200L, 5L);

        // Then
        assertThat(canView).isFalse(); // Private meeting - nie widzi uczestników
    }

    @Test
    void canUserEditMeeting_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canEdit = meetingAuthorizationService.canUserEditMeeting(100L, 1L);

        // Then
        assertThat(canEdit).isTrue();
    }

    @Test
    void canUserDeleteMeeting_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canDelete = meetingAuthorizationService.canUserDeleteMeeting(100L, 1L);

        // Then
        assertThat(canDelete).isTrue();
    }



    @Test
    void canUserManageParticipants_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(publicMeeting));
        when(meetingParticipantService.getParticipantPermissionLevel(100L, 1L))
                .thenReturn(PermissionLevel.ORGANIZER);

        // When
        boolean canManage = meetingAuthorizationService.canUserManageParticipants(100L, 1L);

        // Then
        assertThat(canManage).isTrue();
    }


}