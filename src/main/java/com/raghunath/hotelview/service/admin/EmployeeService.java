package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.entity.Employee;
import com.raghunath.hotelview.entity.EmployeeRefreshToken;
import com.raghunath.hotelview.repository.EmployeeRefreshTokenRepository;
import com.raghunath.hotelview.repository.EmployeeRepository;
import com.raghunath.hotelview.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRefreshTokenRepository employeeRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String registerEmployee(Employee emp, String hotelId) {
        if (employeeRepository.existsByUsername(emp.getUsername())) {
            throw new RuntimeException("Username already taken!");
        }

        emp.setHotelId(hotelId);
        emp.setPassword(passwordEncoder.encode(emp.getPassword()));
        emp.setActive(true);
        // Ensure maxLogins is set (defaulting to 1 if not provided in request)
        if (emp.getMaxLogins() <= 0) emp.setMaxLogins(1);

        employeeRepository.save(emp);
        return "Employee " + emp.getName() + " registered successfully as " + emp.getRole();
    }

    public Map<String, String> login(String username, String password) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!emp.isActive()) throw new RuntimeException("Account is disabled");

        if (passwordEncoder.matches(password, emp.getPassword())) {

            // --- STRICT BLOCK LOGIC ---
            long activeSessions = employeeRefreshTokenRepository.countByUserId(emp.getId());
            if (activeSessions >= emp.getMaxLogins()) {
                throw new RuntimeException("Login limit reached (" + emp.getMaxLogins() + "). Logout elsewhere.");
            }

            // 1. Initialize Version (Start at 1 or use existing if you prefer)
            Long initialVersion = 1L;

            // 2. Pass the 4th argument (version) to fix the error
            String accessToken = jwtUtil.generateAccessToken(emp.getId(), emp.getHotelId(), emp.getRole(), initialVersion);
            String refreshToken = jwtUtil.generateRefreshToken(emp.getId(), emp.getHotelId(), emp.getRole(), initialVersion);

            EmployeeRefreshToken et = EmployeeRefreshToken.builder()
                    .userId(emp.getId())
                    .token(refreshToken)
                    .version(initialVersion) // Save the version in DB
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .build();
            employeeRefreshTokenRepository.save(et);

            return Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "role", emp.getRole(),
                    "name", emp.getName()
            );
        }
        throw new RuntimeException("Invalid credentials");
    }

    public Map<String, String> refreshEmployeeToken(String refreshToken) {
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken.trim())) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        String cleanToken = refreshToken.trim();

        EmployeeRefreshToken storedToken = employeeRefreshTokenRepository.findByToken(cleanToken)
                .orElseThrow(() -> new RuntimeException("Session revoked."));

        String empId = jwtUtil.extractUserId(cleanToken);
        String hotelId = jwtUtil.extractHotelId(cleanToken);
        String role = jwtUtil.extractRole(cleanToken);

        if (!storedToken.getUserId().equals(empId)) {
            throw new RuntimeException("Identity mismatch.");
        }

        // --- VERSION KILLER LOGIC ---
        // 3. Increment the version to kill old access tokens
        Long newVersion = (storedToken.getVersion() != null ? storedToken.getVersion() : 1L) + 1;

        // 4. Pass the newVersion as the 4th argument
        String newAccessToken = jwtUtil.generateAccessToken(empId, hotelId, role, newVersion);
        String newRefreshToken = jwtUtil.generateRefreshToken(empId, hotelId, role, newVersion);

        storedToken.setToken(newRefreshToken);
        storedToken.setVersion(newVersion); // Update version in DB
        storedToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        employeeRefreshTokenRepository.save(storedToken);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    public void logoutEmployee(String empId, String refreshToken) {
        // 1. Trim to avoid Postman copy-paste spaces
        String cleanToken = refreshToken.trim();

        // 2. Call the repository instance (lowercase employeeRefreshTokenRepository)
        Long deletedCount = employeeRefreshTokenRepository.deleteByToken(cleanToken);

        if (deletedCount == 0) {
            System.out.println("DEBUG: No token found for user " + empId + " matching: " + cleanToken);
        } else {
            System.out.println("DEBUG: Successfully deleted " + deletedCount + " session(s)");
        }
    }

    public List<Employee> getMyStaff(String hotelId) {
        return employeeRepository.findAllByHotelId(hotelId);
    }
}