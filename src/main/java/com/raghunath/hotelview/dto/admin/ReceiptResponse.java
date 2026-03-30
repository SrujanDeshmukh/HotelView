package com.raghunath.hotelview.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReceiptResponse {
    // Restaurant Header
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantContact;

    // Order Data
    private String orderId;
    private String date;
    private String time;
    private String orderType;
    private List<FlattenedItem> items;
    private Double grandTotal;

    // Customer Info
    private String customerName;
    private String customerMobile;
    private String customerAddress;

    @Data
    @Builder
    public static class FlattenedItem {
        private String itemName;
        private Integer quantity;
        private Double price;
        private Double subTotal;
    }
}