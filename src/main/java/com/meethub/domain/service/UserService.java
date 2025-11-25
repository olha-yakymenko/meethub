package com.meethub.domain.service;

import com.meethub.domain.model.request.UpdateUserRequest;
import com.meethub.domain.model.response.UserResponse;
import java.util.List;

public interface UserService {
    UserResponse getUserById(Long userId);
    UserResponse getUserByEmail(String email);
    UserResponse updateUser(Long userId, UpdateUserRequest request);
    void deleteUser(Long userId);
    List<UserResponse> searchUsers(String query);
    boolean existsById(Long userId);
}