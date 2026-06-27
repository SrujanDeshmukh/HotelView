package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.CompletedOrderEditDTO;
import com.raghunath.hotelview.dto.admin.DeliverySummaryDTO;
import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.dto.admin.ReceiptResponse;
import com.raghunath.hotelview.entity.Admin;
import com.raghunath.hotelview.entity.CompletedOrder;
import com.raghunath.hotelview.entity.CustomerDetails;
import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.repository.AdminRepository;
import com.raghunath.hotelview.repository.CompleteOrderRepository;
import com.raghunath.hotelview.repository.KitchenOrderRepository;
import com.raghunath.hotelview.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompletedOrderService {

    private final CompleteOrderRepository completeOrderRepository;
    private final AdminRepository adminRepository;
    private final KitchenOrderRepository kitchenOrderRepository;
    private final TableRepository tableRepository;
    private final VersionService versionService;

    @Autowired
    private MongoTemplate mongoTemplate;

    // API 1: Paged Fetch
    public Page<DeliverySummaryDTO> getCompletedOrdersPaged(String hotelId, int pageNumber) {

        // Sort by checkoutDate and checkoutTime since checkoutAt field doesn't exist
        Pageable pageable = PageRequest.of(pageNumber, 10,
                Sort.by("checkoutDate").descending().and(Sort.by("checkoutTime").descending()));

        // 1. Fetch the Entity Page from MongoDB
        Page<CompletedOrder> entities = completeOrderRepository.findByHotelId(hotelId, pageable);

        // 2. Map Entities to DeliverySummaryDTO cleanly
        return entities.map(order -> {
            LocalDateTime dateTimeFallback = LocalDateTime.now();

            if (order.getCheckoutDate() != null && order.getCheckoutTime() != null) {
                try {
                    dateTimeFallback = LocalDateTime.parse(order.getCheckoutDate() + "T" + order.getCheckoutTime());
                } catch (Exception ignored) {
                    // Fallback to current time safely if date string parsing glitches
                }
            }

            return DeliverySummaryDTO.builder()
                    .id(order.getId())
                    .orderType(order.getOrderType())
                    .customerName(order.getCustomerName())
                    .customerMobile(order.getCustomerMobile())
                    .totalPayable(order.getTotalPayable())
                    .checkoutAt(dateTimeFallback)
                    .build();
        });
    }

    public ReceiptResponse getReceiptDetails(String orderId, String hotelIdFromToken) {
        // 1. Fetch the Completed Order
        CompletedOrder order = completeOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security Check: Ensure this order belongs to the hotel in the token
        if (!order.getHotelId().equals(hotelIdFromToken)) {
            throw new RuntimeException("Unauthorized access to this order");
        }

        // 2. Fetch Restaurant Details from Admin Entity
        Admin admin = adminRepository.findByHotelId(hotelIdFromToken)
                .orElseThrow(() -> new RuntimeException("Restaurant profile not found"));

        // 3. Flatten all nested items from allOrders array into one list for the receipt
        List<ReceiptResponse.FlattenedItem> flattenedItems = order.getAllOrders().stream()
                .flatMap(kitchenOrder -> kitchenOrder.getItems().stream())
                .map(item -> ReceiptResponse.FlattenedItem.builder()
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subTotal(item.getSubTotal())
                        .build())
                .collect(Collectors.toList());

        // 4. Build the final Print-Ready response
        return ReceiptResponse.builder()
                .restaurantName(admin.getRestaurantName())
                .restaurantAddress(admin.getRestaurantAddress())
                .restaurantContact(admin.getRestaurantContact())
                .restaurantLogo(admin.getRestaurantLogo())
                .restaurantUpi(admin.getRestaurantUpi())
                .orderId(order.getId())
                .date(order.getCheckoutDate())
                .time(order.getCheckoutTime())
                .orderType(order.getOrderType())
                .items(flattenedItems)
                .grandTotal(order.getGrandTotal())
                .discountPercent(order.getDiscountPercent())
                .discountAmount(order.getDiscountAmount())
                .totalPayable(order.getTotalPayable())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerAddress(order.getCustomerAddress())
                .build();
    }

    public List<CompletedOrder> searchCompletedOrders(String hotelId, String query) {
        return completeOrderRepository.searchOrders(hotelId, query);
    }

    /**
     * FETCH TODAY'S COMPLETED HOME DELIVERIES (Clean Summary)
     */
    public List<DeliverySummaryDTO> getTodayCompletedHomeDeliveries(String hotelId) {
        ZonedDateTime nowIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        String todayDate = nowIST.format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<String> externalTypes = List.of("HOME", "PARCEL");

        // ✅ Fix: Update repository method mapping chain to use checkoutTime sorting
        List<CompletedOrder> orders = completeOrderRepository
                .findByHotelIdAndOrderTypeInAndCheckoutDateOrderByCheckoutDateDescCheckoutTimeDesc(
                        hotelId, externalTypes, todayDate);

        return orders.stream().map(order -> {
            // ✅ Fix: Synthesize LocalDateTime from date/time string maps safely
            LocalDateTime dateTimeFallback = LocalDateTime.now();
            if (order.getCheckoutDate() != null && order.getCheckoutTime() != null) {
                try {
                    dateTimeFallback = LocalDateTime.parse(order.getCheckoutDate() + "T" + order.getCheckoutTime());
                } catch (Exception ignored) {}
            }

            return DeliverySummaryDTO.builder()
                    .id(order.getId())
                    .orderType(order.getOrderType())
                    .customerName(order.getCustomerName())
                    .customerMobile(order.getCustomerMobile())
                    .totalPayable(order.getTotalPayable())
                    .checkoutAt(dateTimeFallback)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 11. EDIT COMPLETED ORDER: Fetches from DB, loops through the nested array items,
     * updates matching item quantities (stored as Strings), recalculates sub-totals/grand-totals,
     * supports a direct manual totalPayable override, and balances customer loyalty spending.
     */
    /**
     * 11. DYNAMIC EDIT COMPLETED ORDER: Fetches from DB, allows quantity updates ONLY for existing items,
     * recalculates item subTotals, and dynamically computes totalPayable using the new discount metrics
     * if no manual custom total payable override is provided.
     */
    @Caching(evict = {
            @CacheEvict(value = "dashboardStatsCache", key = "#hotelId"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-today'"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-week'"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-month'"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-year'")
    })
    @Transactional
    public void editCompletedOrderDetails(String hotelId, String orderId, CompletedOrderEditDTO editDto) {
        // 1. Fetch original order from DB
        CompletedOrder order = completeOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Completed order record not found"));

        if (!order.getHotelId().equals(hotelId)) {
            throw new RuntimeException("Unauthorized action on this resource workspace");
        }

        double oldTotalPayable = order.getTotalPayable() != null ? order.getTotalPayable() : 0.0;

        if (order.getAllOrders() == null || order.getAllOrders().isEmpty()) {
            throw new RuntimeException("This completed order contains no inner order structural array elements");
        }

        List<OrderItem> dbItems = order.getAllOrders().get(0).getItems();

        // 2. Map and update matching item quantities strictly (Only if items array is passed)
        if (editDto.getItems() != null && !editDto.getItems().isEmpty()) {
            for (CompletedOrderEditDTO.UpdatedItemPayload incomingItem : editDto.getItems()) {
                OrderItem originalDbItem = dbItems.stream()
                        .filter(item -> item.getItemName().equalsIgnoreCase(incomingItem.getItemName()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Item '" + incomingItem.getItemName() +
                                "' does not exist in the original bill. Only modifications are allowed."));

                if (incomingItem.getQuantity() <= 0) {
                    throw new RuntimeException("Quantity for '" + incomingItem.getItemName() + "' must be greater than 0.");
                }

                int oldQty = Integer.parseInt(originalDbItem.getQuantity().replaceAll("[^0-9]", ""));
                double unitPrice = originalDbItem.getSubTotal() / oldQty;

                originalDbItem.setQuantity(String.valueOf(incomingItem.getQuantity()));
                originalDbItem.setSubTotal(unitPrice * incomingItem.getQuantity());
                originalDbItem.setPrice(unitPrice);
            }
        }

        // 3. Financial math recalculations
        double newGrandTotal = dbItems.stream().mapToDouble(OrderItem::getSubTotal).sum();

        // Use the new discount percent if provided, otherwise stick to what the order originally had
        double discountPercent = editDto.getDiscountPercent() != null ? editDto.getDiscountPercent() : (order.getDiscountPercent() != null ? order.getDiscountPercent() : 0.0);
        double newDiscountAmount = (newGrandTotal * discountPercent) / 100.0;

        // 🌟 4. Dynamic Total Payable Logic
        double finalTotalPayable;
        if (editDto.getCustomTotalPayable() != null) {
            // Case A: Strict manual override provided by user
            finalTotalPayable = Math.max(0.0, editDto.getCustomTotalPayable());
        } else {
            // Case B: Auto-calculated fallback (Grand Total minus the Discount Percent applied!)
            finalTotalPayable = Math.max(0.0, newGrandTotal - newDiscountAmount);
        }

        // Apply calculated updates back to core root document fields
        order.setGrandTotal(newGrandTotal);
        order.setDiscountPercent(discountPercent);
        order.setDiscountAmount(newDiscountAmount);
        order.setTotalPayable(finalTotalPayable);

        // Sync structural nested sub-total amounts inside the array object tracking node
        order.getAllOrders().get(0).setTotalAmount(newGrandTotal);

        completeOrderRepository.save(order);

        // 5. Adjust Customer analytics metrics with final delta shift balance
        double financialDelta = finalTotalPayable - oldTotalPayable;
        if (StringUtils.hasText(order.getCustomerMobile()) && !"0000000000".equals(order.getCustomerMobile())) {
            Query query = new Query(Criteria.where("hotelId").is(hotelId).and("customerMobile").is(order.getCustomerMobile()));
            Update update = new Update().inc("totalAmountPaid", financialDelta);
            mongoTemplate.updateFirst(query, update, CustomerDetails.class);
        }

        log.info("EDIT_COMPLETED: Order {} processed. Mode: {}, Delta Shift: {}",
                orderId, (editDto.getCustomTotalPayable() != null ? "MANUAL_OVERRIDE" : "AUTO_CALCULATED_DISCOUNT"), financialDelta);
    }

    /**
     * 12. DELETE COMPLETED ORDER: Completely wipes out a bill document from the database collections
     * and reverses the customer's aggregate stats to keep financial indicators synchronized.
     */
    @Caching(evict = {
            @CacheEvict(value = "dashboardStatsCache", key = "#hotelId"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-today'"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-week'"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-month'"),
            @CacheEvict(value = "orderCache", key = "#hotelId + '-year'")
    })
    @Transactional
    public void deleteCompletedOrderById(String hotelId, String orderId) {
        CompletedOrder order = completeOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Completed order not found with ID: " + orderId));

        if (!order.getHotelId().equals(hotelId)) {
            throw new RuntimeException("Unauthorized action. Resource isolation block mismatch.");
        }

        // Deduct history values from Customer profiles
        if (StringUtils.hasText(order.getCustomerMobile()) && !"0000000000".equals(order.getCustomerMobile())) {
            Query query = new Query(Criteria.where("hotelId").is(hotelId).and("customerMobile").is(order.getCustomerMobile()));
            Update update = new Update()
                    .inc("totalOrders", -1)
                    .inc("totalAmountPaid", -order.getTotalPayable());

            mongoTemplate.updateFirst(query, update, CustomerDetails.class);
        }

        // Hard drop from MongoDB collection
        completeOrderRepository.delete(order);
        log.info("DELETE_COMPLETED: Permanently removed finalized order document record ID: {}", orderId);
    }

    @CacheEvict(value = "dashboardStatsCache", key = "#hotelId")
    @Transactional
    public void softDeleteOrders(String hotelId, List<String> orderIds) {
        // 1. Fetch orders from both sources
        List<CompletedOrder> completedOrders = completeOrderRepository.findAllById(orderIds)
                .stream().filter(o -> o.getHotelId().equals(hotelId)).toList();

        List<KitchenOrder> kitchenOrders = kitchenOrderRepository.findAllById(orderIds)
                .stream().filter(o -> o.getHotelId().equals(hotelId)).toList();

        if (completedOrders.isEmpty() && kitchenOrders.isEmpty()) {
            throw new RuntimeException("Unauthorized or Orders not found");
        }

        // ✅ FIX: Map isolated deductions specifically per table to prevent multi-table over-deletion balance wipes!
        Map<String, Double> tableDeductions = new HashMap<>();

        // Process Completed Orders
        for (CompletedOrder o : completedOrders) {
            double amt = (o.getTotalPayable() != null) ? o.getTotalPayable() : 0.0;
            if (o.getAllOrders() != null && !o.getAllOrders().isEmpty()) {
                String tName = o.getAllOrders().get(0).getTableName();
                if (tName != null) {
                    tableDeductions.put(tName, tableDeductions.getOrDefault(tName, 0.0) + amt);
                }
            }
        }

        // Process Kitchen Orders
        for (KitchenOrder o : kitchenOrders) {
            double amt = (o.getTotalAmount() != null) ? o.getTotalAmount() : 0.0;
            if (o.getTableName() != null) {
                tableDeductions.put(o.getTableName(), tableDeductions.getOrDefault(o.getTableName(), 0.0) + amt);
            }
        }

        // 3. Update the RestaurantTable currentBill accurately using your verified string tableName mapping
        tableDeductions.forEach((tableName, deductionForThisTable) -> {
            tableRepository.findByHotelIdAndTableName(hotelId, tableName).ifPresent(table -> {
                double current = (table.getCurrentBill() != null) ? table.getCurrentBill() : 0.0;
                double newBill = Math.max(0, current - deductionForThisTable);

                table.setCurrentBill(newBill);

                // If bill becomes 0, set status to INACTIVE safely
                if (newBill <= 0) {
                    table.setStatus("INACTIVE");
                }

                tableRepository.save(table);
            });
        });

        // 4. Move to Trash with deletedAt IST
        ZonedDateTime nowIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        String deletedAt = nowIST.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<org.bson.Document> trashDocs = new ArrayList<>();

        completedOrders.forEach(order -> {
            org.bson.Document doc = new org.bson.Document();
            mongoTemplate.getConverter().write(order, doc);
            doc.put("deletedAt", deletedAt);
            trashDocs.add(doc);
        });

        kitchenOrders.forEach(order -> {
            org.bson.Document doc = new org.bson.Document();
            mongoTemplate.getConverter().write(order, doc);
            doc.put("deletedAt", deletedAt);
            trashDocs.add(doc);
        });

        // 5. Final DB Operations
        if (!trashDocs.isEmpty()) {
            mongoTemplate.insert(trashDocs, "deleted_orders");
            if (!completedOrders.isEmpty()) completeOrderRepository.deleteAll(completedOrders);
            if (!kitchenOrders.isEmpty()) kitchenOrderRepository.deleteAll(kitchenOrders);
        }

        // 6. Sync Versions
        versionService.bumpSales(hotelId);
        versionService.bumpTables(hotelId);
    }

    public List<org.bson.Document> getDeletedOrders(String hotelId) {
        Query query = new Query(Criteria.where("hotelId").is(hotelId));
        List<org.bson.Document> rawDocs = mongoTemplate.find(query, org.bson.Document.class, "deleted_orders");

        return rawDocs.stream().map(doc -> {
            if (doc.containsKey("_id")) {
                doc.put("id", doc.get("_id").toString());
                doc.remove("_id");
            }
            doc.remove("_class");
            return doc;
        }).collect(Collectors.toList());
    }
}