//
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.model.response.AuthResponse;
//import com.meethub.domain.model.response.UserResponse;
//
//public interface AuthService {
//    UserResponse register(UserRegistrationRequest request);
//    AuthResponse refreshToken(String refreshToke);
//}




package com.meethub.domain.service;

import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.AuthResponse;
import com.meethub.domain.model.response.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface AuthService {

    UserResponse register(
            @Valid
            @NotNull(message = "Dane rejestracji nie mogą być puste")
            UserRegistrationRequest request
    );

}
