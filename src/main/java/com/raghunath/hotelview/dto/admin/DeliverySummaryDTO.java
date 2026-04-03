package com.raghunath.hotelview.dto.admin;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DeliverySummaryDTO {
    private String id;
    private String orderType;
    private String customerName;
    private String customerMobile;
    private Double grandTotal;
    private LocalDateTime checkoutAt;
}