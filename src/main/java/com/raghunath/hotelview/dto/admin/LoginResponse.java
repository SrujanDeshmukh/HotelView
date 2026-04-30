package com.raghunath.hotelview.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String message;
    private String adminId;
    private String hotelId;
    private String accessToken;
    private String refreshToken;

    // Profile Details
    private String name;
    private String mobile;
    private String alternateMobile;
    private String emailId;
    private String restaurantUpi;
    private String address;
    private boolean isPlanExpired;
    private String expiryDate;
}