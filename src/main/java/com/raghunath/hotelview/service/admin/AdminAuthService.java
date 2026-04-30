package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.*;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    // Helper to get current time in IST
    private LocalDateTime getNowIST() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

    public LoginResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByMobile(request.getMobile())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // 1. Subscription check (Already implemented)
        if (getNowIST().isAfter(admin.getSubscriptionExpiry())) {
            throw new RuntimeException("Your subscription plan has ended.");
        }

        // 2. Password check (Already implemented)
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // --- NEW SMART SESSION MANAGEMENT ---

        // Get all active sessions for this hotel, sorted by date (oldest first)
        List<AdminRefreshToken> activeSessions = adminRefreshTokenRepository.findByUserIdOrderByCreatedAtAsc(admin.getHotelId());

        int allowedLogins = (admin.getMaxLogins() > 0) ? admin.getMaxLogins() : 1;

        // If we are at the limit, delete the OLDEST session to make room for the new one
        if (activeSessions.size() >= allowedLogins) {
            // Remove the 0th element (the oldest one)
            AdminRefreshToken oldestSession = activeSessions.get(0);
            adminRefreshTokenRepository.delete(oldestSession);
        }

        // Now proceed to create the new session (This will become the latest/newest)
        return performLogin(admin, "Login successful");
    }

    public LoginResponse register(RegisterRequest request) {
        // 1. Validation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match!");
        }

        if (adminRepository.findByMobile(request.getMobile()).isPresent()) {
            throw new RuntimeException("Account already exists. Please login.");
        }

        // 2. Set Expiry to 7 days from now (IST)
        LocalDateTime subscriptionExpiry = getNowIST().plusDays(7);

        // 3. Create and Save New Admin
        Admin newAdmin = Admin.builder()
                .name(request.getName())
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .hotelId("HOTEL" + System.currentTimeMillis())
                .isApproved(true)
                .isActive(true)
                .subscriptionExpiry(subscriptionExpiry)
                .maxLogins(1)
                .build();

        Admin savedAdmin = adminRepository.save(newAdmin);

        return performLogin(savedAdmin, "Registration successful");
    }

    // Shared logic for Login and Register to generate tokens
    private LoginResponse performLogin(Admin admin, String message) {
        String accessToken = jwtUtil.generateAccessToken(admin.getHotelId(), admin.getHotelId(), "ADMIN");
        String refreshToken = jwtUtil.generateRefreshToken(admin.getHotelId(), admin.getHotelId(), "ADMIN");

        // Save New Session in IST
        AdminRefreshToken adminToken = AdminRefreshToken.builder()
                .userId(admin.getHotelId())
                .token(refreshToken)
                .expiryDate(getNowIST().plusDays(7))
                .build();

        adminRefreshTokenRepository.save(adminToken);

        return LoginResponse.builder()
                .message(message)
                .adminId(admin.getId())
                .hotelId(admin.getHotelId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .name(admin.getName())
                .mobile(admin.getMobile())
                .alternateMobile(admin.getAlternateMobile())
                .emailId(admin.getEmailId())
                .restaurantUpi(admin.getRestaurantUpi())
                .address(admin.getAddress())
                .expiryDate(admin.getSubscriptionExpiry().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")))
                .build();
    }

    public AdminProfileDTO getProfile(String hotelId) {
        Admin admin = adminRepository.findByHotelId(hotelId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found"));

        return AdminProfileDTO.builder()
                .name(admin.getName())
                .mobile(admin.getMobile())
                .alternateMobile(admin.getAlternateMobile())
                .emailId(admin.getEmailId())
                .restaurantUpi(admin.getRestaurantUpi())
                .address(admin.getAddress())
                .subscriptionExpiry(admin.getSubscriptionExpiry().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .build();
    }

    public String updateProfile(String hotelId, AdminProfileDTO updates) {
        Admin admin = adminRepository.findByHotelId(hotelId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found"));

        admin.setName(updates.getName());
        admin.setAlternateMobile(updates.getAlternateMobile());
        admin.setEmailId(updates.getEmailId());
        admin.setRestaurantUpi(updates.getRestaurantUpi());
        admin.setAddress(updates.getAddress());

        adminRepository.save(admin);
        return "Profile updated successfully";
    }

    public Map<String, String> refreshAdminToken(String oldRefreshToken) {
        AdminRefreshToken storedToken = adminRefreshTokenRepository.findByToken(oldRefreshToken.trim())
                .orElseThrow(() -> new RuntimeException("Session revoked or invalid."));

        String hotelId = jwtUtil.extractHotelId(oldRefreshToken);
        Admin admin = adminRepository.findByHotelId(hotelId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Check if subscription expired during the refresh attempt
        if (getNowIST().isAfter(admin.getSubscriptionExpiry())) {
            adminRefreshTokenRepository.delete(storedToken); // Cleanup invalid session
            throw new RuntimeException("Your subscription plan has ended. Kindly upgrade.");
        }

        String newAccess = jwtUtil.generateAccessToken(hotelId, hotelId, "ADMIN");
        String newRefresh = jwtUtil.generateRefreshToken(hotelId, hotelId, "ADMIN");

        // Overwrite the same document to prevent multiple tokens for one user
        storedToken.setToken(newRefresh);
        storedToken.setExpiryDate(getNowIST().plusDays(7));
        adminRefreshTokenRepository.save(storedToken);

        return Map.of(
                "accessToken", newAccess,
                "refreshToken", newRefresh
        );
    }

    @Transactional
    public void logoutAdmin(String refreshToken) {
        adminRefreshTokenRepository.deleteByToken(refreshToken.trim());
    }
}