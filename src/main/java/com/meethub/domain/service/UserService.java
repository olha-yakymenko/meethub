package com.meethub.domain.service;

import com.meethub.domain.model.request.UpdateUserRequest;
import com.meethub.domain.model.response.UserResponse;
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
            @NotBlank @Email String email
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
            @NotBlank @Email String email
    );
}

