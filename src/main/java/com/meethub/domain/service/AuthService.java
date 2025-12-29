// AuthService.java
package com.meethub.domain.service;

import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

public interface AuthService {

    UserResponse register(
            @Valid @NotNull UserRegistrationRequest request
    );
}