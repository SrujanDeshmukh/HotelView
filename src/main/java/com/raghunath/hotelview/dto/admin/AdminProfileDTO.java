package com.raghunath.hotelview.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileDTO {
    private String name;
    private String mobile;
    private String alternateMobile;
    private String emailId;
    private String restaurantUpi;
    private String address;
    private String subscriptionExpiry;
}