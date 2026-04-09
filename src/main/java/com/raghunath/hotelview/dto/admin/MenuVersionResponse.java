package com.raghunath.hotelview.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuVersionResponse {
    private long lastUpdated; // The "Version" timestamp
    private long totalCount;  // Total items in DB
}