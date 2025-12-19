package com.meethub.domain.service;

import com.meethub.domain.model.request.UpdateUserRequest;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.validation.MeethubEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface UserService {

    UserResponse getUserById(
            @NotNull @Positive Long userId
    );

    UserResponse getUserByEmail(
            @NotBlank @Email @MeethubEmail(message = "Email musi być w domenie .com") String email
    );

    UserResponse updateUser(
            @NotNull @Positive Long userId,
            @NotNull UpdateUserRequest request
    );

    void deleteUser(
            @NotNull @Positive Long userId
    );

    List<UserResponse> searchUsers(
            @NotBlank String query
    );

    boolean existsById(
            @NotNull @Positive Long userId
    );

    Long getUserIdByEmail(
            @NotBlank @Email @MeethubEmail(message = "Email musi być w domenie .com") String email
    );
}

