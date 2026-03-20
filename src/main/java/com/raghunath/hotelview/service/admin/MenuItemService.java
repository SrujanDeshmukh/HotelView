package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.MenuItemRequest;
import com.raghunath.hotelview.entity.MenuItem;
import com.raghunath.hotelview.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MongoTemplate mongoTemplate; // Added for Atlas Search

    // 1. ADD ITEM
    public String addMenuItem(MenuItemRequest request, String hotelIdFromToken) {
        MenuItem item = MenuItem.builder()
                .hotelId(hotelIdFromToken)
                .category(request.getCategory())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .isVeg(request.getIsVeg())
                .isAvailable(request.getIsAvailable())
                .imageUrl(request.getImageUrl())
                .preparationTime(request.getPreparationTime())
                .createdAt(LocalDateTime.now())
                .isApproved(true)
                .build();

        menuItemRepository.save(item);
        return "Item added successfully";
    }

    // 2. GET CATEGORY ITEMS
    public List<MenuItem> getCategoryItems(String hotelIdFromToken, String category) {
        return menuItemRepository.findByHotelIdAndCategoryAndIsApprovedTrue(hotelIdFromToken, category);
    }

    public Page<MenuItem> getAllHotelItems(String hotelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return menuItemRepository.findByHotelIdAndIsApprovedTrue(hotelId, pageable);
    }

    /**
     * PRODUCTION GRADE: Smart Search with Typo Tolerance
     * This uses the Atlas Search Index we just created.
     */
    public List<MenuItem> searchMenuItems(String hotelId, String query) {
        // 1. Guard Clause: Protect the DB from 1-letter spam
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        // 2. Define the Search Pipeline
        Document searchStage = new Document("$search", new Document("index", "default")
                .append("compound", new Document()
                        // MUST: The user's autocomplete search (Typo tolerant)
                        .append("must", Collections.singletonList(
                                new Document("autocomplete", new Document("query", query.trim())
                                        .append("path", "name")
                                        .append("tokenOrder", "any")
                                        .append("fuzzy", new Document("maxEdits", 1)))
                        ))
                        // FILTER: Security Lock (Only items for THIS hotel + Only Approved)
                        .append("filter", Arrays.asList(
                                new Document("text", new Document("query", hotelId).append("path", "hotelId")),
                                new Document("equals", new Document("value", true).append("path", "isApproved"))
                        ))
                ));

        // 3. Aggregate and Execute
        Aggregation aggregation = Aggregation.newAggregation(context -> searchStage);

        try {
            return mongoTemplate.aggregate(aggregation, "menu_items", MenuItem.class).getMappedResults();
        } catch (Exception e) {
            // Standard logging for production monitoring
            System.err.println("Search Error for Hotel [" + hotelId + "]: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public MenuItem getMenuItemByHotelAndName(String hotelId, String name) {
        return menuItemRepository.findByHotelIdAndName(hotelId, name)
                .orElseThrow(() -> new RuntimeException("Menu item '" + name + "' not found for this hotel."));
    }
}