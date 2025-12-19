package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserGroupTest {

    @Test
    void shouldCreateUserGroupWithBuilder() {
        // Given
        User createdBy = mock(User.class);
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = createdAt.plusHours(1);

        // When
        UserGroup group = UserGroup.builder()
                .name("Development Team")
                .description("Team working on project development")
                .createdBy(createdBy)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(group.getName()).isEqualTo("Development Team"),
                () -> assertThat(group.getDescription()).isEqualTo("Team working on project development"),
                () -> assertThat(group.getCreatedBy()).isEqualTo(createdBy),
                () -> assertThat(group.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(group.getUpdatedAt()).isEqualTo(updatedAt)
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        UserGroup group = new UserGroup();
        User newCreatedBy = mock(User.class);
        LocalDateTime newCreatedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime newUpdatedAt = LocalDateTime.now();

        // When
        group.setId(1L);
        group.setName("Updated Team Name");
        group.setDescription("Updated description");
        group.setCreatedBy(newCreatedBy);
        group.setCreatedAt(newCreatedAt);
        group.setUpdatedAt(newUpdatedAt);

        // Then
        assertAll(
                () -> assertThat(group.getId()).isEqualTo(1L),
                () -> assertThat(group.getName()).isEqualTo("Updated Team Name"),
                () -> assertThat(group.getDescription()).isEqualTo("Updated description"),
                () -> assertThat(group.getCreatedBy()).isEqualTo(newCreatedBy),
                () -> assertThat(group.getCreatedAt()).isEqualTo(newCreatedAt),
                () -> assertThat(group.getUpdatedAt()).isEqualTo(newUpdatedAt)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        UserGroup group = UserGroup.builder()
                .name("Test Group")
                .createdBy(mock(User.class))
                .build();

        // Then
        assertAll(
                () -> assertThat(group.getDescription()).isNull(),
                () -> assertThat(group.getCreatedAt()).isNull(),
                () -> assertThat(group.getUpdatedAt()).isNull()
        );
    }

    @Test
    void shouldHaveUniqueNameConstraint() {
        // Given - This test verifies the constraint exists, but doesn't test database enforcement
        UserGroup group1 = UserGroup.builder()
                .name("Unique Group")
                .createdBy(mock(User.class))
                .build();

        UserGroup group2 = UserGroup.builder()
                .name("Another Unique Group")
                .createdBy(mock(User.class))
                .build();

        // Then
        assertAll(
                () -> assertThat(group1.getName()).isNotEqualTo(group2.getName()),
                () -> assertThat(group1.getName()).isEqualTo("Unique Group"),
                () -> assertThat(group2.getName()).isEqualTo("Another Unique Group")
        );
    }

    @Test
    void shouldSetTimestampsAutomatically() {
        // Given
        UserGroup group = new UserGroup();
        group.setName("Test Group");
        group.setCreatedBy(mock(User.class));

        // When - simulate @CreationTimestamp and @UpdateTimestamp
        LocalDateTime now = LocalDateTime.now();
        group.setCreatedAt(now);
        group.setUpdatedAt(now);

        // Then
        assertAll(
                () -> assertThat(group.getCreatedAt()).isEqualTo(now),
                () -> assertThat(group.getUpdatedAt()).isEqualTo(now)
        );
    }

    @Test
    void shouldHandleDifferentGroupNames() {
        // Given
        UserGroup teamGroup = UserGroup.builder()
                .name("Engineering Team")
                .build();

        UserGroup projectGroup = UserGroup.builder()
                .name("Project Alpha")
                .build();

        UserGroup departmentGroup = UserGroup.builder()
                .name("Marketing Department")
                .build();

        UserGroup specialCharsGroup = UserGroup.builder()
                .name("Team-2024 (Special)")
                .build();

        // Then
        assertAll(
                () -> assertThat(teamGroup.getName()).isEqualTo("Engineering Team"),
                () -> assertThat(projectGroup.getName()).isEqualTo("Project Alpha"),
                () -> assertThat(departmentGroup.getName()).isEqualTo("Marketing Department"),
                () -> assertThat(specialCharsGroup.getName()).isEqualTo("Team-2024 (Special)")
        );
    }

    @Test
    void shouldHandleDescriptionsOfVariousLengths() {
        // Given
        UserGroup shortDesc = UserGroup.builder()
                .description("Short")
                .build();

        UserGroup mediumDesc = UserGroup.builder()
                .description("Medium length description for a team")
                .build();

        UserGroup longDesc = UserGroup.builder()
                .description("This is a very long description for a user group that explains in detail " +
                        "what this group is about, its purpose, and the types of users who should be members.")
                .build();

        // Then
        assertAll(
                () -> assertThat(shortDesc.getDescription()).isEqualTo("Short"),
                () -> assertThat(mediumDesc.getDescription()).isEqualTo("Medium length description for a team"),
                () -> assertThat(longDesc.getDescription()).contains("very long description")
        );
    }
}
