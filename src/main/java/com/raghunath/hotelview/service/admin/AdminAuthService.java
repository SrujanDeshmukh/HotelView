package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.LoginRequest;
import com.raghunath.hotelview.dto.admin.LoginResponse;
import com.raghunath.hotelview.entity.Admin;
import com.raghunath.hotelview.repository.AdminRepository;
import com.raghunath.hotelview.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;

    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {

        // 1. Find the Admin by their public mobile number
        Admin admin = adminRepository.findByMobile(request.getMobile())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // 2. Security Checks
        if (!admin.isApproved()) {
            throw new RuntimeException("Hotel not approved. Contact Madhava Global.");
        }

        if (!admin.isActive()) {
            throw new RuntimeException("Subscription inactive. Contact Madhava Global.");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // 3. INDUSTRY STANDARD CHANGE:
        // We put the HOTEL ID in the token. Now, the token represents the Business,
        // allowing any authorized staff to manage the same menu.
        String accessToken = jwtUtil.generateAccessToken(admin.getHotelId());
        String refreshToken = jwtUtil.generateRefreshToken(admin.getHotelId());

        return LoginResponse.builder()
                .message("Login successful")
                .adminId(admin.getId())   // Internal Admin ID (for profile)
                .hotelId(admin.getHotelId()) // Unique Hotel ID (for data)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}