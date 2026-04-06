package com.raghunath.hotelview.dto.admin;

import com.raghunath.hotelview.dto.admin.OrderItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private List<OrderItem> items;
    private String comment;
}