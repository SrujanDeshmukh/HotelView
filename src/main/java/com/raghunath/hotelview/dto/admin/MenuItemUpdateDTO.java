package com.raghunath.hotelview.dto.admin;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemUpdateDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    private String description;

    @NotBlank(message = "Short code is required")
    private String shortCode;

    private Boolean isVeg;
    private Boolean isAvailable;
    private String imageUrl;
    private Integer preparationTime;
}