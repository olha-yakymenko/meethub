package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.AccessLevel;
import com.meethub.domain.model.enums.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingResourceTest {

    @Mock private Meeting meeting;
    @Mock private User organizer;
    @Mock private User participant;
    @Mock private User otherUser;
    @Mock private MeetingParticipant meetingParticipant;

    @Test
    void shouldCreateMeetingResourceWithBuilder() {
        // Given
        User uploadedBy = mock(User.class);

        // When
        MeetingResource resource = MeetingResource.builder()
                .meeting(meeting)
                .filename("document.pdf")
                .originalFilename("My Document.pdf")
                .filePath("/uploads/meeting/1/document.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .resourceType(ResourceType.DOCUMENT)
                .uploadedBy(uploadedBy)
                .accessLevel(AccessLevel.PARTICIPANTS)
                .build();

        // Then
        assertAll(
                () -> assertThat(resource.getMeeting()).isEqualTo(meeting),
                () -> assertThat(resource.getFilename()).isEqualTo("document.pdf"),
                () -> assertThat(resource.getOriginalFilename()).isEqualTo("My Document.pdf"),
                () -> assertThat(resource.getFilePath()).isEqualTo("/uploads/meeting/1/document.pdf"),
                () -> assertThat(resource.getFileSize()).isEqualTo(1024L),
                () -> assertThat(resource.getMimeType()).isEqualTo("application/pdf"),
                () -> assertThat(resource.getResourceType()).isEqualTo(ResourceType.DOCUMENT),
                () -> assertThat(resource.getUploadedBy()).isEqualTo(uploadedBy),
                () -> assertThat(resource.getAccessLevel()).isEqualTo(AccessLevel.PARTICIPANTS),
                () -> assertThat(resource.getVersion()).isEqualTo(1),
                () -> assertThat(resource.getIsCurrent()).isTrue(),
                () -> assertThat(resource.getDownloadCount()).isEqualTo(0),
                () -> assertThat(resource.getTags()).isEmpty()
        );
    }

    @Test
    void shouldAddAndRemoveTags() {
        // Given
        MeetingResource resource = new MeetingResource();

        // When
        resource.addTag("important");
        resource.addTag("agenda");
        resource.addTag("presentation");

        // Then
        assertAll(
                () -> assertThat(resource.getTags()).containsExactlyInAnyOrder("important", "agenda", "presentation")
        );

        // When
        resource.removeTag("agenda");

        // Then
        assertAll(
                () -> assertThat(resource.getTags()).containsExactlyInAnyOrder("important", "presentation"),
                () -> assertThat(resource.getTags()).doesNotContain("agenda")
        );
    }

    @Test
    void shouldIncrementDownloadCount() {
        // Given
        MeetingResource resource = new MeetingResource();
        resource.setDownloadCount(5);

        // When
        resource.incrementDownloadCount();

        // Then
        assertAll(
                () -> assertThat(resource.getDownloadCount()).isEqualTo(6)
        );
    }

    @Test
    void shouldIncrementVersion() {
        // Given
        MeetingResource resource = new MeetingResource();
        resource.setVersion(1);

        // When
        resource.incrementVersion();

        // Then
        assertAll(
                () -> assertThat(resource.getVersion()).isEqualTo(2)
        );
    }

    @Test
    void shouldArchiveAndRestoreResource() {
        // Given
        MeetingResource resource = new MeetingResource();
        resource.setIsCurrent(true);

        // When - archive
        resource.archive();

        // Then
        assertAll(
                () -> assertThat(resource.getIsCurrent()).isFalse()
        );

        // When - restore
        resource.restore();

        // Then
        assertAll(
                () -> assertThat(resource.getIsCurrent()).isTrue()
        );
    }

    @Test
    void shouldFormatFileSize() {
        // Test bytes
        MeetingResource smallResource = new MeetingResource();
        smallResource.setFileSize(500L);

        // Test KB
        MeetingResource mediumResource = new MeetingResource();
        mediumResource.setFileSize(1500L);

        // Test MB
        MeetingResource largeResource = new MeetingResource();
        largeResource.setFileSize(2_500_000L);

        // Test GB
        MeetingResource hugeResource = new MeetingResource();
        hugeResource.setFileSize(3_500_000_000L);

        // Test null
        MeetingResource nullResource = new MeetingResource();

        assertAll(
                () -> assertThat(smallResource.getFileSizeFormatted()).isEqualTo("500 B"),
                () -> assertThat(mediumResource.getFileSizeFormatted()).isEqualTo("1,5 KB"),
                () -> assertThat(largeResource.getFileSizeFormatted()).contains("MB"),
                () -> assertThat(hugeResource.getFileSizeFormatted()).contains("GB"),
                () -> assertThat(nullResource.getFileSizeFormatted()).isEqualTo("0 B")
        );
    }

    @Test
    void shouldCheckResourceTypes() {
        // Given
        MeetingResource imageResource = MeetingResource.builder()
                .resourceType(ResourceType.IMAGE)
                .mimeType("image/jpeg")
                .build();

        MeetingResource documentResource = MeetingResource.builder()
                .resourceType(ResourceType.DOCUMENT)
                .mimeType("application/pdf")
                .build();

        MeetingResource presentationResource = MeetingResource.builder()
                .resourceType(ResourceType.PRESENTATION)
                .mimeType("application/vnd.ms-powerpoint")
                .build();

        MeetingResource otherResource = MeetingResource.builder()
                .resourceType(ResourceType.OTHER)
                .mimeType("text/plain")
                .build();

        // Then
        assertAll(
                () -> assertThat(imageResource.isImage()).isTrue(),
                () -> assertThat(imageResource.isDocument()).isFalse(),
                () -> assertThat(imageResource.isPresentation()).isFalse(),

                () -> assertThat(documentResource.isDocument()).isTrue(),
                () -> assertThat(documentResource.isImage()).isFalse(),
                () -> assertThat(documentResource.isPresentation()).isFalse(),

                () -> assertThat(presentationResource.isPresentation()).isTrue(),
                () -> assertThat(presentationResource.isImage()).isFalse(),
                () -> assertThat(presentationResource.isDocument()).isFalse(),

                () -> assertThat(otherResource.isImage()).isFalse(),
                () -> assertThat(otherResource.isDocument()).isFalse(),
                () -> assertThat(otherResource.isPresentation()).isFalse()
        );
    }

    @Test
    void shouldCheckAccessForPublicResource() {
        // Given
        MeetingResource resource = MeetingResource.builder()
                .accessLevel(AccessLevel.PUBLIC)
                .build();

        // Then
        assertAll(
                () -> assertThat(resource.canUserAccess(organizer, meeting)).isTrue(),
                () -> assertThat(resource.canUserAccess(participant, meeting)).isTrue(),
                () -> assertThat(resource.canUserAccess(otherUser, meeting)).isTrue()
        );
    }

    @Test
    void shouldCheckAccessForParticipantsResource() {
        // Given
        MeetingResource resource = MeetingResource.builder()
                .accessLevel(AccessLevel.PARTICIPANTS)
                .uploadedBy(participant)
                .build();

        when(meeting.getOrganizer()).thenReturn(organizer);
        when(meetingParticipant.getUser()).thenReturn(participant);
        when(meetingParticipant.isConfirmed()).thenReturn(true);

        Set<MeetingParticipant> participants = new HashSet<>();
        participants.add(meetingParticipant);
        when(meeting.getParticipants()).thenReturn(participants);

        // Then
        assertAll(
                () -> assertThat(resource.canUserAccess(organizer, meeting)).isTrue(), // Organizer has access
                () -> assertThat(resource.canUserAccess(participant, meeting)).isTrue(), // Confirmed participant
                () -> assertThat(resource.canUserAccess(otherUser, meeting)).isFalse() // Not a participant
        );
    }

    @Test
    void shouldCheckAccessForOrganizersResource() {
        // Given
        MeetingResource resource = MeetingResource.builder()
                .accessLevel(AccessLevel.ORGANIZERS)
                .uploadedBy(organizer)
                .build();

        when(meeting.getOrganizer()).thenReturn(organizer);

        // Then
        assertAll(
                () -> assertThat(resource.canUserAccess(organizer, meeting)).isTrue(),
                () -> assertThat(resource.canUserAccess(participant, meeting)).isFalse(),
                () -> assertThat(resource.canUserAccess(otherUser, meeting)).isFalse()
        );
    }

    @Test
    void shouldCheckAccessForPrivateResource() {
        // Given
        MeetingResource resource = MeetingResource.builder()
                .accessLevel(AccessLevel.PRIVATE)
                .uploadedBy(participant)
                .build();

        when(meeting.getOrganizer()).thenReturn(organizer);

        // Then
        assertAll(
                () -> assertThat(resource.canUserAccess(organizer, meeting)).isTrue(), // Organizer has access to all
                () -> assertThat(resource.canUserAccess(participant, meeting)).isTrue(), // Uploader
                () -> assertThat(resource.canUserAccess(otherUser, meeting)).isFalse() // Not uploader or organizer
        );
    }

    @Test
    void shouldCheckEqualsAndHashCode() {
        // Given
        MeetingResource resource1 = new MeetingResource();
        resource1.setId(1L);
        resource1.setFilename("test.pdf");
        resource1.setMeeting(meeting);

        MeetingResource resource2 = new MeetingResource();
        resource2.setId(1L);
        resource2.setFilename("test.pdf");
        resource2.setMeeting(meeting);

        MeetingResource resource3 = new MeetingResource();
        resource3.setId(2L);
        resource3.setFilename("other.pdf");
        resource3.setMeeting(meeting);

        // Then
        assertAll(
                () -> assertThat(resource1).isEqualTo(resource2),
                () -> assertThat(resource1).isNotEqualTo(resource3),
                () -> assertThat(resource1.hashCode()).isEqualTo(resource2.hashCode()),
                () -> assertThat(resource1.hashCode()).isNotEqualTo(resource3.hashCode())
        );
    }
}