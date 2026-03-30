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
import org.springframework.transaction.annotation.Transactional;

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

        Long initialVersion = 1L;
        // 3. Generate Tokens
        String accessToken = jwtUtil.generateAccessToken(admin.getHotelId(), admin.getHotelId(), "ADMIN", initialVersion);
        String refreshToken = jwtUtil.generateRefreshToken(admin.getHotelId(), admin.getHotelId(), "ADMIN", initialVersion);
        // 4. Save New Session to Database
        AdminRefreshToken adminToken = AdminRefreshToken.builder()
                .userId(admin.getHotelId())
                .token(refreshToken)
                .version(initialVersion) // Ensure this field exists in your RefreshToken entity
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        adminRefreshTokenRepository.save(adminToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Map<String, String> refreshAdminToken(String oldRefreshToken) {
        // 1. Fetch the stored session from DB using the old token string
        AdminRefreshToken storedToken = adminRefreshTokenRepository.findByToken(oldRefreshToken.trim())
                .orElseThrow(() -> new RuntimeException("Session revoked."));

        // 2. EXTRACTION: You must define these variables by extracting them from the token
        String adminId = jwtUtil.extractUserId(oldRefreshToken);
        String hotelId = jwtUtil.extractHotelId(oldRefreshToken);
        String role = jwtUtil.extractRole(oldRefreshToken);

        // 3. Increment version for this specific session
        Long newVersion = (storedToken.getVersion() != null ? storedToken.getVersion() : 1L) + 1;

        // 4. Generate new tokens using the extracted variables and the new version
        String newAccess = jwtUtil.generateAccessToken(adminId, hotelId, role, newVersion);
        String newRefresh = jwtUtil.generateRefreshToken(adminId, hotelId, role, newVersion);

        // 5. Update only THIS session row in the database
        storedToken.setToken(newRefresh);
        storedToken.setVersion(newVersion);
        adminRefreshTokenRepository.save(storedToken);

        return Map.of(
                "accessToken", newAccess,
                "refreshToken", newRefresh
        );
    }
    @Transactional
    public void logoutAdmin(String refreshToken) {
        // Trim the token to remove any accidental spaces/newlines from Postman
        Long deletedCount = adminRefreshTokenRepository.deleteByToken(refreshToken.trim());

        if (deletedCount == 0) {
            System.out.println("DEBUG: No token found in DB matching: " + refreshToken);
            // This confirms if the token was missing or the query failed
        } else {
            System.out.println("DEBUG: Successfully deleted " + deletedCount + " session(s)");
        }
    }
}