package com.raghunath.hotelview.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Document(collection = "menu_items")
public class MenuItem {

    @PersistenceCreator
    public MenuItem() {}

    @Id
    private String id;

    @Field("hotelId")
    private String hotelId;

    @Field("category")
    private String category;

    @Field("name")
    private String name;

    @Field("shortCode")
    private String shortCode;

    @Field("description")
    private String description;

    @Field("price")
    private BigDecimal price;

    @Field("isVeg")
    private Boolean isVeg;

    @Field("isAvailable")
    private Boolean isAvailable;

    @Field("imageUrl")
    private String imageUrl;

    @Field("preparationTime")
    private Integer preparationTime;

    @Field("isApproved")
    private Boolean isApproved;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("updatedAt")
    private LocalDateTime updatedAt;
}