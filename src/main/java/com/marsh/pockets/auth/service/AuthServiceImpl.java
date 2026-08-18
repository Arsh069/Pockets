package com.marsh.pockets.auth.service;

import com.marsh.pockets.auth.dto.AuthResponse;
import com.marsh.pockets.auth.dto.LoginRequest;
import com.marsh.pockets.auth.dto.RefreshRequest;
import com.marsh.pockets.auth.dto.RegisterRequest;
import com.marsh.pockets.auth.entity.RefreshToken;
import com.marsh.pockets.auth.entity.User;
import com.marsh.pockets.auth.repository.RefreshTokenRepository;
import com.marsh.pockets.auth.repository.UserRepository;
import com.marsh.pockets.common.exception.InvalidTokenException;
import com.marsh.pockets.common.exception.UserAlreadyExistsException;
import com.marsh.pockets.config.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long refreshTokenExpiryMs;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            @Value("${jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("User with phone number " + request.phoneNumber() + " already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.phoneNumber(), encodedPassword, request.name(), request.payDay());
        User savedUser = userRepository.save(user);

        return generateAuthTokens(savedUser.getId());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new InvalidTokenException("Invalid phone number or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidTokenException("Invalid phone number or password");
        }

        return generateAuthTokens(user.getId());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        // Step 1: Validate token existence in database (throws 401 if missing)
        RefreshToken refreshTokenObj = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Step 2: Validate token is not revoked (throws 401)
        // REUSE & COMPROMISE DETECTION:
        // Checking `revoked == false` is the core mechanism that makes refresh token rotation secure.
        // If a client attempts to present a token that has ALREADY been marked as revoked (revoked = true),
        // it signifies that the token was previously rotated and used. Re-presenting it suggests token theft
        // or a replay attack. We reject the request immediately with an HTTP 401 Unauthorized error.
        if (refreshTokenObj.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked (possible compromise detected)");
        }

        // Step 3: Validate token is not expired (throws 401)
        if (refreshTokenObj.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // If all validation checks pass, proceed with rotation:
        // Mark the current refresh token as revoked and save it
        refreshTokenObj.setRevoked(true);
        refreshTokenRepository.save(refreshTokenObj);

        // Issue a new access token and new refresh token pair
        return generateAuthTokens(refreshTokenObj.getUserId());
    }

    private AuthResponse generateAuthTokens(Long userId) {
        String accessToken = jwtUtil.generateAccessToken(userId);

        String refreshTokenValue = UUID.randomUUID().toString();
        Instant refreshTokenExpiresAt = Instant.now().plusMillis(refreshTokenExpiryMs);
        RefreshToken newRefreshToken = new RefreshToken(userId, refreshTokenValue, refreshTokenExpiresAt);
        refreshTokenRepository.save(newRefreshToken);

        return new AuthResponse(accessToken, refreshTokenValue, userId);
    }
}
