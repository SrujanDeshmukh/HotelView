package com.raghunath.hotelview.dto.admin;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemSummaryDTO {
    private String id;
    private String category;
    private String name;
    private String shortCode;
    private String description;
    private BigDecimal price;
    private Boolean isVeg;
    private Boolean isAvailable;
    private String imageUrl;
}