package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.UpdateUserRequest;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.meethub.domain.model.enums.UserRole.PARTICIPANT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private final Long USER_ID = 1L;
    private final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail(USER_EMAIL);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhoneNumber("123456789");
        testUser.setRole(PARTICIPANT);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        // When
        UserResponse result = userService.getUserById(USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        assertEquals(USER_EMAIL, result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());

        verify(userRepository, times(1)).findById(USER_ID);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(USER_ID)
        );

        assertEquals("User not found with id: " + USER_ID, exception.getMessage());
        verify(userRepository, times(1)).findById(USER_ID);
    }

    @Test
    void getUserByEmail_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));

        // When
        UserResponse result = userService.getUserByEmail(USER_EMAIL);

        // Then
        assertNotNull(result);
        assertEquals(USER_EMAIL, result.getEmail());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
    }

    @Test
    void getUserByEmail_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserByEmail(USER_EMAIL)
        );

        assertEquals("User not found with email: " + USER_EMAIL, exception.getMessage());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenUserExists() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPhoneNumber("987654321");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = userService.updateUser(USER_ID, request);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, times(1)).save(testUser);

        // Verify that user fields were updated
        assertEquals("Jane", testUser.getFirstName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals("987654321", testUser.getPhoneNumber());
    }

    @Test
    void updateUser_ShouldUpdatePartialFields_WhenSomeFieldsAreNull() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Jane");
        // lastName and phoneNumber are null

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = userService.updateUser(USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals("Jane", testUser.getFirstName()); // Updated
        assertEquals("Doe", testUser.getLastName()); // Unchanged
        assertEquals("123456789", testUser.getPhoneNumber()); // Unchanged

        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserNotFound() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Jane");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUser(USER_ID, request)
        );

        assertEquals("User not found with id: " + USER_ID, exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        // Given
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        // When
        userService.deleteUser(USER_ID);

        // Then
        verify(userRepository, times(1)).existsById(USER_ID);
        verify(userRepository, times(1)).deleteById(USER_ID);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.deleteUser(USER_ID)
        );

        assertEquals("User not found with id: " + USER_ID, exception.getMessage());
        verify(userRepository, never()).deleteById(USER_ID);
    }

    @Test
    void searchUsers_ShouldReturnMatchingUsers() {
        // Given
        String query = "john";
        List<User> users = List.of(testUser);

        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserResponse> result = userService.searchUsers(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getId());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void searchUsers_ShouldReturnEmptyList_WhenNoMatches() {
        // Given
        String query = "nonexistent";
        List<User> users = List.of(testUser);

        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserResponse> result = userService.searchUsers(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchUsers_ShouldBeCaseInsensitive() {
        // Given
        String query = "JOHN"; // uppercase
        List<User> users = List.of(testUser);

        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserResponse> result = userService.searchUsers(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchUsers_ShouldSearchInEmailFirstNameAndLastName() {
        // Given
        User user1 = new User();
        user1.setEmail("user1@test.com");
        user1.setFirstName("Alice");
        user1.setLastName("Johnson");

        User user2 = new User();
        user2.setEmail("user2@test.com");
        user2.setFirstName("Bob");
        user2.setLastName("Smith");

        List<User> users = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When - search by email
        List<UserResponse> resultByEmail = userService.searchUsers("user1");
        // When - search by first name
        List<UserResponse> resultByFirstName = userService.searchUsers("Alice");
        // When - search by last name
        List<UserResponse> resultByLastName = userService.searchUsers("Johnson");

        // Then
        assertEquals(1, resultByEmail.size());
        assertEquals(1, resultByFirstName.size());
        assertEquals(1, resultByLastName.size());
    }

    @Test
    void existsById_ShouldReturnTrue_WhenUserExists() {
        // Given
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        // When
        boolean result = userService.existsById(USER_ID);

        // Then
        assertTrue(result);
        verify(userRepository, times(1)).existsById(USER_ID);
    }

    @Test
    void existsById_ShouldReturnFalse_WhenUserNotExists() {
        // Given
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        // When
        boolean result = userService.existsById(USER_ID);

        // Then
        assertFalse(result);
        verify(userRepository, times(1)).existsById(USER_ID);
    }
}