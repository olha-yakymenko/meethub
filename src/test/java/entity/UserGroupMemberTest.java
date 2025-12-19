package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserGroupMemberTest {

    @Test
    void shouldCreateUserGroupMemberWithBuilder() {
        // Given
        UserGroup group = mock(UserGroup.class);
        User user = mock(User.class);
        LocalDateTime joinedAt = LocalDateTime.now();

        // When
        UserGroupMember member = UserGroupMember.builder()
                .group(group)
                .user(user)
                .role("MEMBER")
                .joinedAt(joinedAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(member.getGroup()).isEqualTo(group),
                () -> assertThat(member.getUser()).isEqualTo(user),
                () -> assertThat(member.getRole()).isEqualTo("MEMBER"),
                () -> assertThat(member.getJoinedAt()).isEqualTo(joinedAt)
        );
    }


    @Test
    void shouldUpdateAllFields() {
        // Given
        UserGroupMember member = new UserGroupMember();
        UserGroup newGroup = mock(UserGroup.class);
        User newUser = mock(User.class);
        LocalDateTime newJoinedAt = LocalDateTime.now().minusDays(1);

        // When
        member.setId(1L);
        member.setGroup(newGroup);
        member.setUser(newUser);
        member.setRole("ADMIN");
        member.setJoinedAt(newJoinedAt);

        // Then
        assertAll(
                () -> assertThat(member.getId()).isEqualTo(1L),
                () -> assertThat(member.getGroup()).isEqualTo(newGroup),
                () -> assertThat(member.getUser()).isEqualTo(newUser),
                () -> assertThat(member.getRole()).isEqualTo("ADMIN"),
                () -> assertThat(member.getJoinedAt()).isEqualTo(newJoinedAt)
        );
    }

    @Test
    void shouldHandleDifferentRoles() {
        // Given
        UserGroupMember admin = UserGroupMember.builder()
                .role("ADMIN")
                .build();

        UserGroupMember moderator = UserGroupMember.builder()
                .role("MODERATOR")
                .build();

        UserGroupMember member = UserGroupMember.builder()
                .role("MEMBER")
                .build();

        UserGroupMember viewer = UserGroupMember.builder()
                .role("VIEWER")
                .build();

        // Then
        assertAll(
                () -> assertThat(admin.getRole()).isEqualTo("ADMIN"),
                () -> assertThat(moderator.getRole()).isEqualTo("MODERATOR"),
                () -> assertThat(member.getRole()).isEqualTo("MEMBER"),
                () -> assertThat(viewer.getRole()).isEqualTo("VIEWER")
        );
    }

    @Test
    void shouldSetJoinedAtAutomatically() {
        // Given
        UserGroupMember member = new UserGroupMember();
        member.setGroup(mock(UserGroup.class));
        member.setUser(mock(User.class));

        // When - simulate @CreationTimestamp
        LocalDateTime now = LocalDateTime.now();
        member.setJoinedAt(now);

        // Then
        assertAll(
                () -> assertThat(member.getJoinedAt()).isEqualTo(now)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        UserGroupMember member = new UserGroupMember();

        // Then
        assertAll(
                () -> assertThat(member.getGroup()).isNull(),
                () -> assertThat(member.getUser()).isNull(),
                () -> assertThat(member.getJoinedAt()).isNull()
        );
    }

    @Test
    void shouldCreateMemberWithCustomRole() {
        // Given
        UserGroupMember customRoleMember = UserGroupMember.builder()
                .group(mock(UserGroup.class))
                .user(mock(User.class))
                .role("CONTRIBUTOR")
                .build();

        // Then
        assertAll(
                () -> assertThat(customRoleMember.getRole()).isEqualTo("CONTRIBUTOR")
        );
    }
}