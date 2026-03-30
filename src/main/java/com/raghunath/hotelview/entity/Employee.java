package com.raghunath.hotelview.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "employees")
public class Employee {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username; // Employee unique login name

    private String password;

    private String hotelId; // Linked to the Admin's Hotel ID

    private String name;

    private String role; // "WAITER" or "CHEF"

    private boolean isActive = true;

    private int maxLogins = 1;

    private Long tokenVersion = 1L;
}