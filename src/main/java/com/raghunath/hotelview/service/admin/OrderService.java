package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.CheckoutRequest;
import com.raghunath.hotelview.dto.admin.ReceiptResponse;
import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.entity.Admin;
import com.raghunath.hotelview.entity.CompletedOrder;
import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.entity.OrderDraft;
import com.raghunath.hotelview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDraftRepository draftRepository;
    private final AdminRepository adminRepository;
    private final TableRepository tableRepository;
    private final KitchenOrderRepository kitchenOrderRepository;
    private final MongoTemplate mongoTemplate;
    private final CompleteOrderRepository completeOrderRepository;
    /**
     * 1. SAVE DRAFT: Waiter/Admin adds items.
     * Since the waiter has full access, this updates the live table total immediately.
     */
    public void saveDraft(String hotelId, int tableNumber, List<OrderItem> items) {
        validateTableExists(hotelId, tableNumber);
        Double total = items.stream().mapToDouble(OrderItem::getSubTotal).sum();

        OrderDraft draft = draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber)
                .orElse(OrderDraft.builder()
                        .hotelId(hotelId)
                        .tableNumber(tableNumber)
                        .build());

        draft.setItems(items);
        draft.setTotalAmount(total);
        draft.setUpdatedAt(LocalDateTime.now());
        draftRepository.save(draft);
    }

    private ZonedDateTime getISTNow() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    private void validateTableExists(String hotelId, int tableNumber) {
        if (!tableRepository.existsByHotelIdAndTableNumber(hotelId, tableNumber)) {
            log.error("VALIDATION_FAILED: Table {} not found for Hotel {}", tableNumber, hotelId);
            throw new RuntimeException("Invalid Table Number: " + tableNumber);
        }
    }

    /**
     * 2. FETCH DRAFT: View current unsent items.
     */
    public OrderDraft getDraft(String hotelId, int tableNumber) {
        return draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).orElse(null);
    }

    /**
     * 3. CONFIRM ORDER: Moves items to the Kitchen.
     * Table status becomes 'PENDING' to alert the Chef.
     */
    @Transactional
    public String confirmOrder(String hotelId, int tableNumber, List<OrderItem> items, String waiterId) {
        validateTableExists(hotelId, tableNumber);
        ZonedDateTime nowIST = getISTNow();
        Double total = items.stream().mapToDouble(OrderItem::getSubTotal).sum();

        KitchenOrder kOrder = KitchenOrder.builder()
                .hotelId(hotelId)
                .tableNumber(tableNumber)
                .orderType("TABLE")
                .items(items)
                .totalAmount(total)
                .status("PENDING") // The order is pending
                .createdBy(waiterId)
                .createdAt(nowIST.toLocalDateTime())
                .createdDate(nowIST.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .createdTime(nowIST.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .build();

        kitchenOrderRepository.save(kOrder);
        draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(draftRepository::delete);

        // CRITICAL: New order always pushes Table status back to PENDING
        updateTableVisualStatus(hotelId, tableNumber, "PENDING");

        return "Table order sent to kitchen";
    }

    @Transactional
    public String confirmHomeDelivery(String hotelId, List<OrderItem> items, String waiterId) {
        ZonedDateTime nowIST = getISTNow();
        Double total = items.stream().mapToDouble(OrderItem::getSubTotal).sum();

        KitchenOrder deliveryOrder = KitchenOrder.builder()
                .hotelId(hotelId)
                .tableNumber(null) // Keep null for delivery
                .orderType("HOME_DELIVERY")
                .items(items)
                .totalAmount(total)
                .status("PENDING")
                .createdBy(waiterId)
                .createdAt(nowIST.toLocalDateTime())
                .createdDate(nowIST.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .createdTime(nowIST.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .build();

        return kitchenOrderRepository.save(deliveryOrder).getId();
    }
    /**
     * 4. FETCH TABLE ORDERS: Latest orders first.
     */
    public List<KitchenOrder> getOrdersByTable(String hotelId, int tableNumber) {
        return kitchenOrderRepository.findByHotelIdAndTableNumberAndStatusNotOrderByCreatedAtDesc(
                hotelId, tableNumber, "PAID");
    }

    /**
     * 5. UPDATE STATUS WITH CHEF: Handles Kitchen Lifecycle.
     * PREPARING -> Table shows 'PREPARING'
     * COMPLETED -> Table shows 'ACTIVE' (Food is served/Guest eating)
     */
    public void updateStatusWithChef(String orderId, String newStatus, String userId) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Query query = new Query(Criteria.where("id").is(orderId));
        Update update = new Update();
        update.set("status", newStatus.toUpperCase());

        if ("PREPARING".equalsIgnoreCase(newStatus)) {
            update.set("acceptedBy", userId);
        }

        mongoTemplate.updateFirst(query, update, KitchenOrder.class);

        // Syncing Table Status
        if ("TABLE".equalsIgnoreCase(order.getOrderType()) && order.getTableNumber() != null) {
            String tableUIStatus;

            switch (newStatus.toUpperCase()) {
                case "ACCEPTED":
                case "PREPARING":
                    tableUIStatus = "ACCEPTED";
                    break;
                case "COMPLETED":
                    tableUIStatus = "ACTIVE"; // Chef finished, customer is eating
                    break;
                default:
                    tableUIStatus = newStatus.toUpperCase();
            }

            updateTableVisualStatus(order.getHotelId(), order.getTableNumber(), tableUIStatus);
        }
    }/**
     * 6. GENERAL STATUS UPDATE: Fallback for direct status changes.
     */
    public void updateOrderStatus(String orderId, String newStatus) {
        updateStatusWithChef(orderId, newStatus, null);
    }

    @Transactional
    public String checkoutOrders(String hotelId, CheckoutRequest request) {
        // 1. Fetch active orders from the kitchen
        List<KitchenOrder> activeOrders = kitchenOrderRepository.findAllById(request.getOrderIds());
        if (activeOrders.isEmpty()) {
            throw new RuntimeException("No orders found to checkout for these IDs");
        }

        // 2. Calculate final totals and timing in IST
        Double grandTotal = activeOrders.stream().mapToDouble(KitchenOrder::getTotalAmount).sum();
        ZonedDateTime nowIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

        // 3. Build the Archive Document (CompletedOrder)
        CompletedOrder finalBill = CompletedOrder.builder()
                .hotelId(hotelId)
                .orderType(activeOrders.get(0).getOrderType())
                .customerName(request.getCustomerName())
                .customerMobile(request.getCustomerMobile())
                .customerAddress(request.getCustomerAddress())
                .allOrders(activeOrders) // Full nesting of original kitchen orders
                .grandTotal(grandTotal)
                .paymentStatus("PAID")
                .checkoutAt(nowIST.toLocalDateTime())
                .checkoutDate(nowIST.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .checkoutTime(nowIST.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .build();

        // 4. Save to history collection
        CompletedOrder savedBill = completeOrderRepository.save(finalBill);

        // 5. ATOMIC CLEANUP & TABLE RESET
        if (savedBill.getId() != null) {
            // Remove active orders from kitchen display
            kitchenOrderRepository.deleteAll(activeOrders);

            // Reset the Physical Table Status to INACTIVE
            Integer tableNum = activeOrders.get(0).getTableNumber();
            if ("TABLE".equalsIgnoreCase(activeOrders.get(0).getOrderType()) && tableNum != null) {

                // We find and reset the table's visual status AND its current bill
                tableRepository.findByHotelIdAndTableNumber(hotelId, tableNum).ifPresent(t -> {
                    t.setStatus("INACTIVE"); // Per your requirement: InActive after checkout
                    t.setCurrentBill(0.0);   // Reset bill to zero for next guest
                    tableRepository.save(t);
                    log.info("TABLE_RESET: Hotel {} Table {} is now INACTIVE", hotelId, tableNum);
                });
            }
        }

        return "Success";
    }
    // --- API 1: Paged Fetch (5 at a time) ---
    public Page<CompletedOrder> getCompletedOrdersPaged(String hotelId, int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, 5, Sort.by("checkoutAt").descending());
        return completeOrderRepository.findByHotelId(hotelId, pageable);
    }

    // --- API 2: Full Detail by ID ---
    public CompletedOrder getCompletedOrderDetails(String id) {
        return completeOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order history not found for ID: " + id));
    }

    // --- API 3: Search by Name or Mobile ---
    public List<CompletedOrder> searchCompletedOrders(String hotelId, String query) {
        return completeOrderRepository.searchOrders(hotelId, query);
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
        Admin admin = (Admin) adminRepository.findByHotelId(hotelIdFromToken)
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
                .orderId(order.getId())
                .date(order.getCheckoutDate())
                .time(order.getCheckoutTime())
                .orderType(order.getOrderType())
                .items(flattenedItems)
                .grandTotal(order.getGrandTotal())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerAddress(order.getCustomerAddress())
                .build();
    }
    /**
     * HELPER: Syncs the physical Table entity with the digital order status.
     */
    private void updateTableVisualStatus(String hotelId, Integer tableNumber, String status) {
        if (tableNumber == null) return; // Never update table UI for delivery

        tableRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(t -> {
            t.setStatus(status);
            tableRepository.save(t);
        });
    }

}