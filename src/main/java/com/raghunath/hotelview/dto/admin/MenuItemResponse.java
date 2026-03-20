package com.raghunath.hotelview.dto.admin;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {
    private String id;
    private String name;
    private String category;
    private BigDecimal price;
    private Boolean isVeg;
    private Boolean isAvailable;
    private String imageUrl;
}