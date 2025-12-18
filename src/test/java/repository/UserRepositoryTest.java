package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        String email = "test.user@example.com";

        Optional<User> userOpt = userRepository.findByEmail(email);

        assertAll("User by email",
                () -> assertThat(userOpt).isPresent(),
                () -> assertThat(userOpt).map(User::getEmail).hasValue(email),
                () -> assertThat(userOpt).map(User::getFirstName).hasValue("User")
        );
    }

    @Test
    @DisplayName("Should check if email exists")
    void testExistsByEmail() {
        boolean exists = userRepository.existsByEmail("test.admin@example.com");
        boolean notExists = userRepository.existsByEmail("non.existing@example.com");

        assertAll("Email existence check",
                () -> assertThat(exists).isTrue(),
                () -> assertThat(notExists).isFalse()
        );
    }

    @Test
    @DisplayName("Should find users by email, first name or last name containing keyword")
    void testFindByEmailContainingOrFirstNameContainingOrLastNameContaining() {
        List<User> users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                "test.user", "Admin", "Test"
        );

        assertAll("Users matching keyword",
                () -> assertThat(users).isNotEmpty(),
                () -> users.forEach(user ->
                        assertThat(user.getEmail().contains("test.user") ||
                                user.getFirstName().contains("Admin") ||
                                user.getLastName().contains("Test")).isTrue()
                )
        );
    }

    @Test
    @DisplayName("Should save a new user")
    void testSaveUser() {
        User newUser = new User();
        newUser.setEmail("new.user@example.com");
        newUser.setPassword("pass");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setRole(UserRole.MODERATOR);
        newUser.setEnabled(true);

        User savedUser = userRepository.save(newUser);

        assertAll("Saved user properties",
                () -> assertThat(savedUser.getId()).isNotNull(),
                () -> assertThat(savedUser.getEmail()).isEqualTo("new.user@example.com"),
                () -> assertThat(savedUser.getFirstName()).isEqualTo("New"),
                () -> assertThat(savedUser.getLastName()).isEqualTo("User"),
                () -> assertThat(savedUser.getRole()).isEqualTo(UserRole.MODERATOR),
                () -> assertThat(savedUser.isEnabled()).isTrue()
        );
    }
}
