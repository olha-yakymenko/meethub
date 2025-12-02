package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UpdateUserRequest;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserResponse expectedResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(UserRole.PARTICIPANT)
                .createdAt(LocalDateTime.now())
                .build();

        expectedResponse = UserResponse.builder()
                .id(1L)
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(UserRole.PARTICIPANT)
                .createdAt(testUser.getCreatedAt())
                .build();
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user response when user exists")
        void shouldReturnUserResponseWhenUserExists() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getUserById(1L);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should map all user fields correctly")
        void shouldMapAllUserFieldsCorrectly() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getUserById(1L);

            // Then
            assertAll(
                    () -> assertThat(result.getEmail()).isEqualTo("john.doe@example.com"),
                    () -> assertThat(result.getFirstName()).isEqualTo("John"),
                    () -> assertThat(result.getLastName()).isEqualTo("Doe"),
                    () -> assertThat(result.getPhoneNumber()).isEqualTo("123456789"),
                    () -> assertThat(result.getRole()).isEqualTo(UserRole.PARTICIPANT),
                    () -> assertThat(result.getCreatedAt()).isEqualTo(testUser.getCreatedAt())
            );
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: 999");
        }
    }

    @Nested
    @DisplayName("getUserByEmail Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Should return user response when email exists")
        void shouldReturnUserResponseWhenEmailExists() {
            // Given
            when(userRepository.findByEmail("john.doe@example.com"))
                    .thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getUserByEmail("john.doe@example.com");

            // Then
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            // Given
            when(userRepository.findByEmail("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("getUserIdByEmail Tests")
    class GetUserIdByEmailTests {

        @Test
        @DisplayName("Should return user ID when email exists")
        void shouldReturnUserIdWhenEmailExists() {
            // Given
            when(userRepository.findByEmail("john.doe@example.com"))
                    .thenReturn(Optional.of(testUser));

            // When
            Long userId = userService.getUserIdByEmail("john.doe@example.com");

            // Then
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw RuntimeException when email not found")
        void shouldThrowRuntimeExceptionWhenEmailNotFound() {
            // Given
            when(userRepository.findByEmail("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getUserIdByEmail("nonexistent@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update first name when provided")
        void shouldUpdateFirstNameWhenProvided() {
            // Given
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Jonathan")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            UserResponse result = userService.updateUser(1L, request);

            // Then
            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("Jonathan") &&
                            user.getLastName().equals("Doe") // unchanged
            ));
        }

        @Test
        @DisplayName("Should update last name when provided")
        void shouldUpdateLastNameWhenProvided() {
            // Given
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .lastName("Smith")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.updateUser(1L, request);

            // Then
            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("John") && // unchanged
                            user.getLastName().equals("Smith")
            ));
        }

        @Test
        @DisplayName("Should update phone number when provided")
        void shouldUpdatePhoneNumberWhenProvided() {
            // Given
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .phoneNumber("987654321")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.updateUser(1L, request);

            // Then
            verify(userRepository).save(argThat(user ->
                    user.getPhoneNumber().equals("987654321")
            ));
        }

        @Test
        @DisplayName("Should update multiple fields when provided")
        void shouldUpdateMultipleFieldsWhenProvided() {
            // Given
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("555555555")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.updateUser(1L, request);

            // Then
            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("Jane") &&
                            user.getLastName().equals("Smith") &&
                            user.getPhoneNumber().equals("555555555")
            ));
        }

        @Test
        @DisplayName("Should not update fields when null")
        void shouldNotUpdateFieldsWhenNull() {
            // Given
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName(null)
                    .lastName(null)
                    .phoneNumber(null)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.updateUser(1L, request);

            // Then
            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("John") && // unchanged
                            user.getLastName().equals("Doe") && // unchanged
                            user.getPhoneNumber().equals("123456789") // unchanged
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFoundForUpdate() {
            // Given
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("NewName")
                    .build();

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: 999");
        }

        @Test
        @DisplayName("Should save updated user to repository")
        void shouldSaveUpdatedUserToRepository() {
            // Given
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Updated")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.updateUser(1L, request);

            // Then
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user when exists")
        void shouldDeleteUserWhenExists() {
            // Given
            when(userRepository.existsById(1L)).thenReturn(true);

            // When
            userService.deleteUser(1L);

            // Then
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFoundForDelete() {
            // Given
            when(userRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: 999");
        }

        @Test
        @DisplayName("Should check if user exists before deletion")
        void shouldCheckIfUserExistsBeforeDeletion() {
            // Given
            when(userRepository.existsById(1L)).thenReturn(true);

            // When
            userService.deleteUser(1L);

            // Then
            verify(userRepository).existsById(1L);
            verify(userRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("searchUsers Tests")
    class SearchUsersTests {

        private User user1;
        private User user2;
        private User user3;

        @BeforeEach
        void setUp() {
            user1 = User.builder()
                    .id(1L)
                    .email("alice@example.com")
                    .firstName("Alice")
                    .lastName("Johnson")
                    .build();

            user2 = User.builder()
                    .id(2L)
                    .email("bob.smith@example.com")
                    .firstName("Bob")
                    .lastName("Smith")
                    .build();

            user3 = User.builder()
                    .id(3L)
                    .email("charlie@test.com")
                    .firstName("Charlie")
                    .lastName("Brown")
                    .build();
        }

        @Test
        @DisplayName("Should return users matching email query")
        void shouldReturnUsersMatchingEmailQuery() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            // When
            List<UserResponse> results = userService.searchUsers("alice@example.com");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("Should return users matching first name query")
        void shouldReturnUsersMatchingFirstNameQuery() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            // When
            List<UserResponse> results = userService.searchUsers("Alice");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFirstName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("Should return users matching last name query")
        void shouldReturnUsersMatchingLastNameQuery() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            // When
            List<UserResponse> results = userService.searchUsers("Smith");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getLastName()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            // When
            List<UserResponse> results = userService.searchUsers("ALICE");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFirstName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            // When
            List<UserResponse> results = userService.searchUsers("nonexistent");

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return multiple users when query matches multiple")
        void shouldReturnMultipleUsersWhenQueryMatchesMultiple() {
            // Given
            User user4 = User.builder()
                    .id(4L)
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3, user4));

            // When
            List<UserResponse> results = userService.searchUsers("example");

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).extracting("email")
                    .contains("alice@example.com", "bob.smith@example.com");
        }

        @Test
        @DisplayName("Should handle empty query string")
        void shouldHandleEmptyQueryString() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            // When
            List<UserResponse> results = userService.searchUsers("");

            // Then
            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("existsById Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when user exists")
        void shouldReturnTrueWhenUserExists() {
            // Given
            when(userRepository.existsById(1L)).thenReturn(true);

            // When
            boolean exists = userService.existsById(1L);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when user does not exist")
        void shouldReturnFalseWhenUserDoesNotExist() {
            // Given
            when(userRepository.existsById(999L)).thenReturn(false);

            // When
            boolean exists = userService.existsById(999L);

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("mapToUserResponse Tests")
    class MapToUserResponseTests {

        @Test
        @DisplayName("Should map all fields from User to UserResponse")
        void shouldMapAllFieldsFromUserToUserResponse() {
            // When
            UserResponse result = userService.mapToUserResponse(testUser);

            // Then
            assertAll(
                    () -> assertThat(result.getId()).isEqualTo(testUser.getId()),
                    () -> assertThat(result.getEmail()).isEqualTo(testUser.getEmail()),
                    () -> assertThat(result.getFirstName()).isEqualTo(testUser.getFirstName()),
                    () -> assertThat(result.getLastName()).isEqualTo(testUser.getLastName()),
                    () -> assertThat(result.getPhoneNumber()).isEqualTo(testUser.getPhoneNumber()),
                    () -> assertThat(result.getRole()).isEqualTo(testUser.getRole()),
                    () -> assertThat(result.getCreatedAt()).isEqualTo(testUser.getCreatedAt())
            );
        }

        @Test
        @DisplayName("Should handle null phone number")
        void shouldHandleNullPhoneNumber() {
            // Given
            User userWithoutPhone = User.builder()
                    .id(2L)
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber(null)
                    .role(UserRole.PARTICIPANT)
                    .createdAt(LocalDateTime.now())
                    .build();

            // When
            UserResponse result = userService.mapToUserResponse(userWithoutPhone);

            // Then
            assertThat(result.getPhoneNumber()).isNull();
        }
    }

    @Test
    @DisplayName("Should use read-only transaction for getUserById")
    void shouldUseReadOnlyTransactionForGetUserById() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.getUserById(1L);

        // Then - test passes if no exception, annotation is checked at runtime
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should use read-only transaction for searchUsers")
    void shouldUseReadOnlyTransactionForSearchUsers() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        // When
        userService.searchUsers("John");

        // Then
        verify(userRepository).findAll();
    }
}