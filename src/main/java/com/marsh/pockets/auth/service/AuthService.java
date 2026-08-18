package com.marsh.pockets.auth.service;

import com.marsh.pockets.auth.dto.AuthResponse;
import com.marsh.pockets.auth.dto.LoginRequest;
import com.marsh.pockets.auth.dto.RefreshRequest;
import com.marsh.pockets.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);
}
