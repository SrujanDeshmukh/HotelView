package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.entity.AdminRefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AdminRefreshTokenRepository extends MongoRepository<AdminRefreshToken, String> {

    @Query("{ 'token' : ?0 }")
    Optional<AdminRefreshToken> findByToken(String token);

    @Query("{ 'userId' : ?0 }")
    List<AdminRefreshToken> findByUserId(String userId);

    // ✅ This is where the Filter was crashing. ?0 bypasses the name requirement.
    @Query(value = "{ 'userId' : ?0 }", exists = true)
    boolean existsByUserId(String userId);

    @Query(value = "{ 'userId' : ?0 }", count = true)
    long countByUserId(String userId);

    @Query(delete = true, value = "{ 'userId' : ?0 }")
    void deleteByUserId(String userId);

    @Query(delete = true, value = "{ 'token' : ?0 }")
    long deleteByToken(String token);
}