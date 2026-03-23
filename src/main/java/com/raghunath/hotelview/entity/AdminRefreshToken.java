package com.raghunath.hotelview.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "admin_refresh_tokens") // or "employee_refresh_tokens"
public class AdminRefreshToken {
    @Id
    private String id;
    private String userId;   // Admin ID or Employee ID
    private String token;    // The actual JWT
    private LocalDateTime expiryDate;
}