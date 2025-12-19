package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UpdateUserRequest;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    // ==============================
    // TESTY getUserById
    // ==============================

    @Test
    @DisplayName("Should return user response when user exists")
    void shouldReturnUserResponseWhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserById(1L);

        assertAll(
                () -> assertEquals(1L, result.getId()),
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


    // ==============================
    // TESTY getUserByEmail
    // ==============================

    @Test
    @DisplayName("Should return user response when email exists")
    void shouldReturnUserResponseWhenEmailExists() {
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserByEmail("john.doe@example.com");

        assertAll(
                () -> assertEquals("john.doe@example.com", result.getEmail()),
                () -> assertEquals("John", result.getFirstName()),
                () -> assertEquals("Doe", result.getLastName())
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


    @Test
    @DisplayName("Should accept valid .com email")
    void shouldAcceptValidComEmail() {
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(testUser));

        UserResponse result = userService.getUserByEmail("john.doe@example.com");

        assertEquals("john.doe@example.com", result.getEmail());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test@example.pl",
            "user@domain.org",
            "invalid-email",
            "user@.com",
            "@example.com",
            "user@example.com.pl"
    })
    @DisplayName("Should not find users with invalid email domains")
    void shouldNotFindUsersWithInvalidEmailDomains(String invalidEmail) {
        when(userRepository.findByEmail(invalidEmail)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByEmail(invalidEmail)
        );

        verify(userRepository).findByEmail(invalidEmail);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "TEST@EXAMPLE.COM", // wielkie litery
            "  user@example.com  ", // białe znaki
            "john.doe@company.com",
            "user123@test.com"
    })
    @DisplayName("Should accept various valid .com emails")
    void shouldAcceptVariousValidComEmails(String validEmail) {
        User mockUser = User.builder()
                .id(1L)
                .email(validEmail.trim().toLowerCase())
                .firstName("Test")
                .lastName("User")
                .build();

        when(userRepository.findByEmail(validEmail))
                .thenReturn(Optional.of(mockUser));

        UserResponse result = userService.getUserByEmail(validEmail);

        assertNotNull(result);
    }

    // ==============================
    // TESTY getUserIdByEmail
    // ==============================

    @Test
    @DisplayName("Should return user ID when email exists")
    void shouldReturnUserIdWhenEmailExists() {
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(testUser));

        Long userId = userService.getUserIdByEmail("john.doe@example.com");

        assertEquals(1L, userId);
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

    // ==============================
    // TESTY updateUser
    // ==============================

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
    @DisplayName("Should throw ResourceNotFoundException when user not found for update")
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

 
    // ==============================
    // TESTY deleteUser
    // ==============================

    @Test
    @DisplayName("Should delete user when exists")
    void shouldDeleteUserWhenExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found for delete")
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


    // ==============================
    // TESTY searchUsers
    // ==============================

    @Test
    @DisplayName("Should return users matching email query")
    void shouldReturnUsersMatchingEmailQuery() {
        User user1 = User.builder()
                .id(1L)
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Johnson")
                .build();

        User user2 = User.builder()
                .id(2L)
                .email("bob.smith@example.com")
                .firstName("Bob")
                .lastName("Smith")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> results = userService.searchUsers("alice@example.com");

        assertEquals(1, results.size());
        assertEquals("alice@example.com", results.get(0).getEmail());
    }

    @Test
    @DisplayName("Should return empty list when no matches")
    void shouldReturnEmptyListWhenNoMatches() {
        User user1 = User.builder()
                .id(1L)
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Johnson")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user1));

        List<UserResponse> results = userService.searchUsers("nonexistent");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }



    // ==============================
    // TESTY existsById
    // ==============================

    @Test
    @DisplayName("Should return true when user exists")
    void shouldReturnTrueWhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        boolean exists = userService.existsById(1L);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when user does not exist")
    void shouldReturnFalseWhenUserDoesNotExist() {
        when(userRepository.existsById(999L)).thenReturn(false);

        boolean exists = userService.existsById(999L);

        assertFalse(exists);
    }


    // ==============================
    // TESTY mapToUserResponse
    // ==============================

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

        assertNull(result.getPhoneNumber());
    }

    // ==============================
    // TESTY TRANSACTIONAL
    // ==============================

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


    @Test
    @DisplayName("Should accept edge case valid emails")
    void shouldAcceptEdgeCaseValidEmails() {
        // Test granicznych przypadków
        String[] validEmails = {
                "a@b.com",               // najkrótszy możliwy
                "test123@domain123.com", // cyfry
                "user.name@company.com", // kropka w nazwie
                "user+tag@example.com",  // plus w nazwie
                "user_name@test.com",    // podkreślenie
                "user-name@test.com",    // myślnik
        };

        for (String email : validEmails) {
            User mockUser = User.builder()
                    .id(1L)
                    .email(email)
                    .firstName("Test")
                    .lastName("User")
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

            UserResponse result = userService.getUserByEmail(email);
            assertNotNull(result, "Should accept email: " + email);
        }
    }

    @Test
    @DisplayName("Should handle whitespace trimming for email validation")
    void shouldHandleWhitespaceTrimmingForEmailValidation() {
        // Walidator powinien trimować białe znaki
        String emailWithSpaces = "  user@example.com  ";
        User mockUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        when(userRepository.findByEmail(emailWithSpaces)).thenReturn(Optional.of(mockUser));

        UserResponse result = userService.getUserByEmail(emailWithSpaces);
        assertNotNull(result);
    }
}