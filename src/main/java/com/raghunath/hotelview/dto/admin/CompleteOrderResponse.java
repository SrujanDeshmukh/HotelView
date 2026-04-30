package com.raghunath.hotelview.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompleteOrderResponse {
    private boolean success;
    private String message;
    private OrderData data;

    @Data
    @Builder
    public static class OrderData {
        private String order_id;
        private Double grand_total;
        private String receipt_url;
        private String customer_name;
    }
}