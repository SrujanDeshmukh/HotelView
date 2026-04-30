package com.raghunath.hotelview.dto.admin;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String mobile;
    private String password;
    private String confirmPassword;
}
