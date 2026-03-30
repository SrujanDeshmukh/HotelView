package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.entity.AdminRefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AdminRefreshTokenRepository extends MongoRepository<AdminRefreshToken, String> {

    // Find a specific session by its refresh token (Used in /refresh-token)
    Optional<AdminRefreshToken> findByToken(String token);

    // Fetch ALL active sessions for a hotel (Used in the Filter for version checking)
    // Changing this to List prevents the "non unique result" crash
    List<AdminRefreshToken> findByUserId(String userId);

    // Used to enforce the Premium Plan limits (e.g., max 5 devices)
    long countByUserId(String userId);

    // Cleanup methods
    void deleteByUserId(String userId);
    long deleteByToken(String token);
}