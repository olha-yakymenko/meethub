package com.meethub.domain.model.response;

import com.meethub.domain.model.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String phoneNumber;
    private LocalDateTime createdAt;

    // GETTERS
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public UserRole getRole() { return role; }
    public String getPhoneNumber() { return phoneNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setRole(UserRole role) { this.role = role; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Konstruktory
    public UserResponse() {}

    public UserResponse(Long id, String email, String firstName, String lastName, UserRole role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public UserResponse(Long id, String email, String firstName, String lastName,
                        UserRole role, String phoneNumber, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    // Metody pomocnicze
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAdmin() {
        return UserRole.ADMIN.equals(role);
    }

    public boolean isOrganizer() {
        return UserRole.ORGANIZER.equals(role) || UserRole.ADMIN.equals(role);
    }

    public boolean isParticipant() {
        return UserRole.PARTICIPANT.equals(role);
    }

    public boolean hasPhoneNumber() {
        return phoneNumber != null && !phoneNumber.trim().isEmpty();
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserResponse that = (UserResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    // toString
    @Override
    public String toString() {
        return "UserResponse{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                ", phoneNumber='" + (phoneNumber != null ? "***" : "null") + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // Builder pattern
    public static UserResponseBuilder builder() {
        return new UserResponseBuilder();
    }

    public static class UserResponseBuilder {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private UserRole role;
        private String phoneNumber;
        private LocalDateTime createdAt;

        public UserResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserResponseBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserResponseBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserResponseBuilder role(UserRole role) {
            this.role = role;
            return this;
        }

        public UserResponseBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponse build() {
            UserResponse response = new UserResponse();
            response.setId(id);
            response.setEmail(email);
            response.setFirstName(firstName);
            response.setLastName(lastName);
            response.setRole(role);
            response.setPhoneNumber(phoneNumber);
            response.setCreatedAt(createdAt);
            return response;
        }
    }
}