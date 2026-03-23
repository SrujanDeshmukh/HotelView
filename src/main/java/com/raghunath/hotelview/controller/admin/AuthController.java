package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.LoginRequest;
import com.raghunath.hotelview.dto.admin.LoginResponse;
import com.raghunath.hotelview.service.admin.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        return adminAuthService.login(request);

    }
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(adminAuthService.refreshAdminToken(request.get("refreshToken")));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        adminAuthService.logoutAdmin(adminId);
        return ResponseEntity.ok("Admin logged out successfully");
    }
}