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

}