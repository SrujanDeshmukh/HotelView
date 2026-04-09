package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.MenuItemRequest;
import com.raghunath.hotelview.dto.admin.MenuItemSummaryDTO; // 👈 New DTO
import com.raghunath.hotelview.dto.admin.MenuItemUpdateDTO;
import com.raghunath.hotelview.dto.admin.MenuVersionResponse;
import com.raghunath.hotelview.entity.MenuItem;
import com.raghunath.hotelview.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;


    public String addMenuItem(MenuItemRequest request, String hotelIdFromToken) {
        LocalDateTime now = LocalDateTime.now();
        MenuItem item = MenuItem.builder()
                .hotelId(hotelIdFromToken)
                .category(request.getCategory())
                .name(request.getName())
                .shortCode(request.getShortCode())
                .description(request.getDescription())
                .price(request.getPrice())
                .isVeg(request.getIsVeg())
                .isAvailable(request.getIsAvailable())
                .imageUrl(request.getImageUrl())
                .preparationTime(request.getPreparationTime())
                .createdAt(now)
                .updatedAt(now) // 👈 Critical for Version Check
                .isApproved(true)
                .build();
        menuItemRepository.save(item);
        return "Item added successfully with code: " + request.getShortCode();
    }


    public List<MenuItemSummaryDTO> searchMenuItems(String hotelId, String query) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }

        return menuItemRepository.findByHotelIdAndSearchQuery(hotelId, query.trim())
                .stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }


    public MenuItemSummaryDTO patchMenuItem(String hotelId, String itemId, java.util.Map<String, Object> updates) {
        MenuItem existingItem = menuItemRepository.findByHotelIdAndId(hotelId, itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        updates.forEach((key, value) -> {
            switch (key) {
                case "price" -> existingItem.setPrice(new java.math.BigDecimal(value.toString()));
                case "isAvailable" -> existingItem.setIsAvailable((Boolean) value);

            }
        });

        existingItem.setUpdatedAt(java.time.LocalDateTime.now());
        MenuItem savedItem = menuItemRepository.save(existingItem);


        return MenuItemSummaryDTO.builder()
                .id(savedItem.getId())
                .name(savedItem.getName())
                .price(savedItem.getPrice())
                .shortCode(savedItem.getShortCode())
                .category(savedItem.getCategory())
                .isAvailable(savedItem.getIsAvailable())
                .isVeg(savedItem.getIsVeg())
                .imageUrl(savedItem.getImageUrl())
                .description(savedItem.getDescription())
                .build();
    }


    public List<MenuItemSummaryDTO> getAllItemsForCache(String hotelId) {
        return menuItemRepository.findAllByHotelIdForCache(hotelId)
                .stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }


    public long getLatestUpdateTimestamp(String hotelId) {
        return menuItemRepository.findTopByHotelIdOrderByUpdatedAtDesc(hotelId)
                .map(item -> {
                    LocalDateTime latest = (item.getUpdatedAt() != null) ? item.getUpdatedAt() : item.getCreatedAt();
                    return latest.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                })
                .orElse(0L);
    }


    private MenuItemSummaryDTO convertToSummaryDto(MenuItem item) {
        return MenuItemSummaryDTO.builder()
                .id(item.getId())
                .category(item.getCategory())
                .name(item.getName())
                .shortCode(item.getShortCode())
                .description(item.getDescription())
                .price(item.getPrice())
                .isVeg(item.getIsVeg())
                .isAvailable(item.getIsAvailable())
                .imageUrl(item.getImageUrl())
                .build();
    }


    public List<MenuItem> getCategoryItems(String hotelIdFromToken, String category) {
        return menuItemRepository.findByHotelIdAndCategoryAndIsApprovedTrue(hotelIdFromToken, category);
    }


    public MenuItem getMenuItemByHotelAndName(String hotelId, String name) {
        return menuItemRepository.findByHotelIdAndName(hotelId, name)
                .orElseThrow(() -> new RuntimeException("Menu item '" + name + "' not found for this hotel."));
    }


    public MenuItem toggleAvailability(String hotelId, String itemId, boolean available) {
        MenuItem item = menuItemRepository.findByHotelIdAndId(hotelId, itemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found with id: " + itemId));

        item.setIsAvailable(available);
        item.setUpdatedAt(java.time.LocalDateTime.now()); // 👈 This is the "version trigger"

        return menuItemRepository.save(item);
    }


    public Page<MenuItem> getAllHotelItems(String hotelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return menuItemRepository.findByHotelIdAndIsApprovedTrue(hotelId, pageable);
    }


    public MenuItemSummaryDTO toggleAvailabilityAndReturnDto(String hotelId, String itemId, boolean available) {
        MenuItem item = toggleAvailability(hotelId, itemId, available);
        return convertToSummaryDto(item);
    }


    public MenuItemSummaryDTO updateMenuItem(String hotelId, String itemId, MenuItemUpdateDTO dto) {

        MenuItem existingItem = menuItemRepository.findByHotelIdAndId(hotelId, itemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found or unauthorized access"));


        existingItem.setName(dto.getName());
        existingItem.setCategory(dto.getCategory());
        existingItem.setPrice(dto.getPrice());
        existingItem.setDescription(dto.getDescription());
        existingItem.setShortCode(dto.getShortCode());
        existingItem.setIsVeg(dto.getIsVeg() != null ? dto.getIsVeg() : existingItem.getIsVeg());
        existingItem.setIsAvailable(dto.getIsAvailable() != null ? dto.getIsAvailable() : existingItem.getIsAvailable());
        existingItem.setImageUrl(dto.getImageUrl());
        existingItem.setPreparationTime(dto.getPreparationTime());

        existingItem.setUpdatedAt(LocalDateTime.now());

        MenuItem savedItem = menuItemRepository.save(existingItem);

        return convertToSummaryDto(savedItem);
    }


    public MenuVersionResponse getMenuMetadata(String hotelId) {
        long version = menuItemRepository.findTopByHotelIdOrderByUpdatedAtDesc(hotelId)
                .map(item -> {
                    LocalDateTime latest = (item.getUpdatedAt() != null) ? item.getUpdatedAt() : item.getCreatedAt();
                    return latest.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                })
                .orElse(0L);


        long count = menuItemRepository.countByHotelId(hotelId);

        return new MenuVersionResponse(version, count);
    }


    public List<MenuItem> getChangedItems(String hotelId, long lastSyncMillis) {
        LocalDateTime lastSync = java.time.Instant.ofEpochMilli(lastSyncMillis)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

        return menuItemRepository.findByHotelIdAndUpdatedAtGreaterThan(hotelId, lastSync);
    }
}