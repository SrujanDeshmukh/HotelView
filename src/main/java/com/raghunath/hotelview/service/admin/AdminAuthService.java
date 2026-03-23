package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.LoginRequest;
import com.raghunath.hotelview.dto.admin.LoginResponse;
import com.raghunath.hotelview.entity.Admin;
import com.raghunath.hotelview.entity.AdminRefreshToken;
import com.raghunath.hotelview.repository.AdminRefreshTokenRepository;
import com.raghunath.hotelview.repository.AdminRepository;
import com.raghunath.hotelview.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {

        Admin admin = adminRepository.findByMobile(request.getMobile())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // 1. Basic Security Checks
        if (!admin.isApproved()) throw new RuntimeException("Hotel not approved.");
        if (!admin.isActive()) throw new RuntimeException("Subscription inactive.");

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // --- 2. STRICT BLOCK LOGIC ---
        // Count existing sessions for this hotelId/admin
        long activeSessions = adminRefreshTokenRepository.countByUserId(admin.getHotelId());

        // Use maxLogins from entity (default to 1 if not set)
        int allowedLogins = (admin.getMaxLogins() > 0) ? admin.getMaxLogins() : 1;

        if (activeSessions >= allowedLogins) {
            throw new RuntimeException("Login limit reached (" + allowedLogins +
                    "). Please logout from another device first.");
        }

        // 3. Generate Tokens
        String accessToken = jwtUtil.generateAccessToken(admin.getHotelId(), admin.getHotelId(), "ADMIN");
        String refreshToken = jwtUtil.generateRefreshToken(admin.getHotelId(), admin.getHotelId(), "ADMIN");

        // 4. Save New Session to Database
        AdminRefreshToken adminToken = AdminRefreshToken.builder()
                .userId(admin.getHotelId())
                .token(refreshToken)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        adminRefreshTokenRepository.save(adminToken);

        return LoginResponse.builder()
                .message("Login successful")
                .adminId(admin.getId())
                .hotelId(admin.getHotelId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Map<String, String> refreshAdminToken(String oldRefreshToken) {
        // 1. Physical Validation
        if (!jwtUtil.validateToken(oldRefreshToken)) {
            throw new RuntimeException("Refresh token expired");
        }

        // 2. Database Check (Security Layer)
        AdminRefreshToken storedToken = adminRefreshTokenRepository.findByToken(oldRefreshToken)
                .orElseThrow(() -> new RuntimeException("Admin session invalid or logged out"));

        // 3. Extract Claims
        String adminId = jwtUtil.extractUserId(oldRefreshToken);
        String hotelId = jwtUtil.extractHotelId(oldRefreshToken);
        String role = jwtUtil.extractRole(oldRefreshToken);

        // --- 4. TOKEN ROTATION ---
        // Generate a new pair
        String newAccessToken = jwtUtil.generateAccessToken(adminId, hotelId, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(adminId, hotelId, role);

        // Update existing record with the new refresh token (keeps session count stable)
        storedToken.setToken(newRefreshToken);
        storedToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        adminRefreshTokenRepository.save(storedToken);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    public void logoutAdmin(String refreshToken) {
        // Industry Standard: Precise Logout
        // Deletes ONLY the specific session tied to this token
        adminRefreshTokenRepository.deleteByToken(refreshToken);
    }
}