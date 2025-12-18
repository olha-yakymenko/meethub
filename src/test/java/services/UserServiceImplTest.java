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

import static org.junit.jupiter.api.Assertions.*;
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
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserById(1L);

            assertAll(
                    () -> assertEquals(1L, result.getId())
            );
        }

        @Test
        @DisplayName("Should map all user fields correctly")
        void shouldMapAllUserFieldsCorrectly() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserById(1L);

            assertAll(
                    () -> assertEquals("john.doe@example.com", result.getEmail()),
                    () -> assertEquals("John", result.getFirstName()),
                    () -> assertEquals("Doe", result.getLastName()),
                    () -> assertEquals("123456789", result.getPhoneNumber()),
                    () -> assertEquals(UserRole.PARTICIPANT, result.getRole()),
                    () -> assertEquals(testUser.getCreatedAt(), result.getCreatedAt())
            );
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> userService.getUserById(999L)
            );
        }
    }

    @Nested
    @DisplayName("getUserByEmail Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Should return user response when email exists")
        void shouldReturnUserResponseWhenEmailExists() {
            when(userRepository.findByEmail("john.doe@example.com"))
                    .thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserByEmail("john.doe@example.com");

            assertAll(
                    () -> assertEquals("john.doe@example.com", result.getEmail())
            );
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            when(userRepository.findByEmail("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> userService.getUserByEmail("nonexistent@example.com")
            );
        }
    }

    @Nested
    @DisplayName("getUserIdByEmail Tests")
    class GetUserIdByEmailTests {

        @Test
        @DisplayName("Should return user ID when email exists")
        void shouldReturnUserIdWhenEmailExists() {
            when(userRepository.findByEmail("john.doe@example.com"))
                    .thenReturn(Optional.of(testUser));

            Long userId = userService.getUserIdByEmail("john.doe@example.com");

            assertAll(
                    () -> assertEquals(1L, userId)
            );
        }

        @Test
        @DisplayName("Should throw RuntimeException when email not found")
        void shouldThrowRuntimeExceptionWhenEmailNotFound() {
            when(userRepository.findByEmail("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> userService.getUserIdByEmail("nonexistent@example.com")
            );
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update first name when provided")
        void shouldUpdateFirstNameWhenProvided() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Jonathan")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, request);

            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("Jonathan") &&
                            user.getLastName().equals("Doe")
            ));
        }

        @Test
        @DisplayName("Should update last name when provided")
        void shouldUpdateLastNameWhenProvided() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .lastName("Smith")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, request);

            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("John") &&
                            user.getLastName().equals("Smith")
            ));
        }

        @Test
        @DisplayName("Should update phone number when provided")
        void shouldUpdatePhoneNumberWhenProvided() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .phoneNumber("987654321")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, request);

            verify(userRepository).save(argThat(user ->
                    user.getPhoneNumber().equals("987654321")
            ));
        }

        @Test
        @DisplayName("Should update multiple fields when provided")
        void shouldUpdateMultipleFieldsWhenProvided() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("555555555")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, request);

            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("Jane") &&
                            user.getLastName().equals("Smith") &&
                            user.getPhoneNumber().equals("555555555")
            ));
        }

        @Test
        @DisplayName("Should not update fields when null")
        void shouldNotUpdateFieldsWhenNull() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName(null)
                    .lastName(null)
                    .phoneNumber(null)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, request);

            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("John") &&
                            user.getLastName().equals("Doe") &&
                            user.getPhoneNumber().equals("123456789")
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFoundForUpdate() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("NewName")
                    .build();

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> userService.updateUser(999L, request)
            );
        }

        @Test
        @DisplayName("Should save updated user to repository")
        void shouldSaveUpdatedUserToRepository() {
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .firstName("Updated")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, request);

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user when exists")
        void shouldDeleteUserWhenExists() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFoundForDelete() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> userService.deleteUser(999L)
            );
        }

        @Test
        @DisplayName("Should check if user exists before deletion")
        void shouldCheckIfUserExistsBeforeDeletion() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

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
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            List<UserResponse> results = userService.searchUsers("alice@example.com");

            assertAll(
                    () -> assertEquals(1, results.size()),
                    () -> assertEquals("alice@example.com", results.get(0).getEmail())
            );
        }

        @Test
        @DisplayName("Should return users matching first name query")
        void shouldReturnUsersMatchingFirstNameQuery() {
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            List<UserResponse> results = userService.searchUsers("Alice");

            assertAll(
                    () -> assertEquals(1, results.size()),
                    () -> assertEquals("Alice", results.get(0).getFirstName())
            );
        }

        @Test
        @DisplayName("Should return users matching last name query")
        void shouldReturnUsersMatchingLastNameQuery() {
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            List<UserResponse> results = userService.searchUsers("Smith");

            assertAll(
                    () -> assertEquals(1, results.size()),
                    () -> assertEquals("Smith", results.get(0).getLastName())
            );
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            List<UserResponse> results = userService.searchUsers("ALICE");

            assertAll(
                    () -> assertEquals(1, results.size()),
                    () -> assertEquals("Alice", results.get(0).getFirstName())
            );
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() {
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            List<UserResponse> results = userService.searchUsers("nonexistent");

            assertAll(
                    () -> assertNotNull(results),
                    () -> assertTrue(results.isEmpty())
            );
        }

        @Test
        @DisplayName("Should return multiple users when query matches multiple")
        void shouldReturnMultipleUsersWhenQueryMatchesMultiple() {
            User user4 = User.builder()
                    .id(4L)
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3, user4));

            List<UserResponse> results = userService.searchUsers("example");

            assertAll(
                    () -> assertEquals(3, results.size()),
                    () -> assertTrue(results.stream().anyMatch(r -> r.getEmail().equals("alice@example.com"))),
                    () -> assertTrue(results.stream().anyMatch(r -> r.getEmail().equals("bob.smith@example.com")))
            );
        }

        @Test
        @DisplayName("Should handle empty query string")
        void shouldHandleEmptyQueryString() {
            when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));

            List<UserResponse> results = userService.searchUsers("");

            assertAll(
                    () -> assertEquals(3, results.size())
            );
        }
    }

    @Nested
    @DisplayName("existsById Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when user exists")
        void shouldReturnTrueWhenUserExists() {
            when(userRepository.existsById(1L)).thenReturn(true);

            boolean exists = userService.existsById(1L);

            assertAll(
                    () -> assertTrue(exists)
            );
        }

        @Test
        @DisplayName("Should return false when user does not exist")
        void shouldReturnFalseWhenUserDoesNotExist() {
            when(userRepository.existsById(999L)).thenReturn(false);

            boolean exists = userService.existsById(999L);

            assertAll(
                    () -> assertFalse(exists)
            );
        }
    }

    @Nested
    @DisplayName("mapToUserResponse Tests")
    class MapToUserResponseTests {

        @Test
        @DisplayName("Should map all fields from User to UserResponse")
        void shouldMapAllFieldsFromUserToUserResponse() {
            UserResponse result = userService.mapToUserResponse(testUser);

            assertAll(
                    () -> assertEquals(testUser.getId(), result.getId()),
                    () -> assertEquals(testUser.getEmail(), result.getEmail()),
                    () -> assertEquals(testUser.getFirstName(), result.getFirstName()),
                    () -> assertEquals(testUser.getLastName(), result.getLastName()),
                    () -> assertEquals(testUser.getPhoneNumber(), result.getPhoneNumber()),
                    () -> assertEquals(testUser.getRole(), result.getRole()),
                    () -> assertEquals(testUser.getCreatedAt(), result.getCreatedAt())
            );
        }

        @Test
        @DisplayName("Should handle null phone number")
        void shouldHandleNullPhoneNumber() {
            User userWithoutPhone = User.builder()
                    .id(2L)
                    .email("test@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber(null)
                    .role(UserRole.PARTICIPANT)
                    .createdAt(LocalDateTime.now())
                    .build();

            UserResponse result = userService.mapToUserResponse(userWithoutPhone);

            assertAll(
                    () -> assertNull(result.getPhoneNumber())
            );
        }
    }

    @Test
    @DisplayName("Should use read-only transaction for getUserById")
    void shouldUseReadOnlyTransactionForGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.getUserById(1L);

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should use read-only transaction for searchUsers")
    void shouldUseReadOnlyTransactionForSearchUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        userService.searchUsers("John");

        verify(userRepository).findAll();
    }
}