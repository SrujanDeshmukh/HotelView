package com.raghunath.hotelview.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstantCheckoutRequestNew {
    private List<OrderItem> items; // Will accept custom items cleanly
    private Double discountPercent;
    private String customerName;
    private String customerMobile;
    private String customerAddress;
}