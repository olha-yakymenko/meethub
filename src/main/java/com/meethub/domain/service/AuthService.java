//package com.meethub.domain.service;
//
//import com.meethub.domain.model.request.LoginRequest;
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.model.response.AuthResponse;
//import com.meethub.domain.model.response.UserResponse;
//
//public interface AuthService {
//    UserResponse register(UserRegistrationRequest request);
//    AuthResponse login(LoginRequest request);
//    AuthResponse refreshToken(String refreshToken);
//    void logout(String token);
//}



package com.meethub.domain.service;

import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.AuthResponse;
import com.meethub.domain.model.response.UserResponse;

public interface AuthService {
    UserResponse register(UserRegistrationRequest request);
    AuthResponse refreshToken(String refreshToke);
}