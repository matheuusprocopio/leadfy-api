package com.leadfy.api.service;

import com.leadfy.api.dto.request.LoginRequest;
import com.leadfy.api.dto.request.RegisterRequest;
import com.leadfy.api.dto.response.AuthResponse;

public interface AuthService {

	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);
}
