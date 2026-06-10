package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.entity.RestaurantTable;
import com.raghunath.hotelview.repository.KitchenOrderRepository;
import com.raghunath.hotelview.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final KitchenOrderRepository kitchenOrderRepository;
    private final VersionService versionService;

    public List<RestaurantTable> getAllTables(String hotelId) {
        return tableRepository.findAllByHotelIdOrderByTableNameAsc(hotelId);
    }

    @CacheEvict(value = "dashboardStatsCache", key = "#hotelId")
    public RestaurantTable saveTable(String hotelId, RestaurantTable table) {
        table.setHotelId(hotelId);
        if (table.getStatus() == null) table.setStatus("INACTIVE");
        if (table.getCurrentBill() == null) table.setCurrentBill(0.0);
        table.setUpdatedAt(LocalDateTime.now()); // ✅ Set updatedAt
        return tableRepository.save(table);
    }

    // 🔥 CRITICAL ADDITION: Add to transferTableOrders
    @CacheEvict(value = "dashboardStatsCache", key = "#hotelId")
    @Transactional
    public void transferTableOrders(String hotelId, String fromTable, String toTable) {
        // 1. Validate that the destination table exists
        RestaurantTable targetTable;
        try {
            targetTable = tableRepository.findByHotelIdAndTableName(hotelId, toTable)
                    .orElseThrow(() -> new RuntimeException("Target table " + toTable + " does not exist"));
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            // 🎯 CATCHES THE DUPLICATE ERROR HERE AND RETURNS YOUR CUSTOM MESSAGE
            throw new RuntimeException("Data Error: Multiple tables exist with the name '" + toTable + "'. Please clean your table data.");
        }
        // 2. Fetch all active kitchen orders for the source table
        List<KitchenOrder> activeOrders = kitchenOrderRepository.findByHotelIdAndTableName(hotelId, fromTable);

        if (activeOrders.isEmpty()) {
            throw new RuntimeException("No active orders found on Table " + fromTable);
        }

        // 3. Calculate the total amount being moved
        double transferAmount = activeOrders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();

        // 4. Update the table number in each kitchen order
        activeOrders.forEach(order -> {
            order.setTableName(toTable);
            // Optional: you can also update the 'updatedAt' timestamp here
        });
        kitchenOrderRepository.saveAll(activeOrders);

        // 5. Update Source Table: Deduct bill and set to AVAILABLE if empty
        tableRepository.findByHotelIdAndTableName(hotelId, fromTable).ifPresent(source -> {
            double current = source.getCurrentBill() != null ? source.getCurrentBill() : 0.0;
            double newBill = Math.max(0, current - transferAmount);
            source.setCurrentBill(newBill);
            if (newBill <= 0) source.setStatus("AVAILABLE");
            tableRepository.save(source);
        });

        // 6. Update Target Table: Add bill and set to OCCUPIED/PENDING
        double targetCurrent = targetTable.getCurrentBill() != null ? targetTable.getCurrentBill() : 0.0;
        targetTable.setCurrentBill(targetCurrent + transferAmount);
        targetTable.setStatus("PENDING"); // Or "OCCUPIED" based on your logic
        tableRepository.save(targetTable);

        // 7. Sync Versions so Waiter Apps update immediately
        versionService.bumpTables(hotelId);
    }

    // Add to deleteTable
    @CacheEvict(value = "dashboardStatsCache", key = "#hotelId")
    public void deleteTable(String id, String hotelId) {
        // 1. Fetch by the true Document ID
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + id));

        // 2. Add an ownership security guardrail check
        if (!table.getHotelId().equals(hotelId)) {
            throw new RuntimeException("Unauthorized: You cannot delete this table");
        }

        // 3. Prevent deleting active tables with running tabs
        if (table.getCurrentBill() != null && table.getCurrentBill() > 0) {
            throw new RuntimeException("Cannot delete table while it has an active unpaid bill of ₹" + table.getCurrentBill());
        }

        // 4. Drop from database safely
        tableRepository.delete(table);

        // 5. Bump versions so waiter apps sync out deleted layouts instantly
        versionService.bumpTables(hotelId);
    }

    @CacheEvict(value = "dashboardStatsCache", key = "#hotelId")
    public RestaurantTable updateTable(String id, String hotelId,
                                       RestaurantTable details) {
        RestaurantTable existingTable = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Table not found with id: " + id));

        if (!existingTable.getHotelId().equals(hotelId)) {
            throw new RuntimeException(
                    "Unauthorized: This table does not belong to your hotel");
        }

        if (details.getTableName() != null)
            existingTable.setTableName(details.getTableName());
        if (details.getSeatingCapacity() != null)
            existingTable.setSeatingCapacity(details.getSeatingCapacity());
        if (details.getStatus() != null)
            existingTable.setStatus(details.getStatus());

        existingTable.setUpdatedAt(LocalDateTime.now()); // ✅ Set updatedAt
        return tableRepository.save(existingTable);
    }

//    public void deleteTable(String id, String hotelId) {
//        RestaurantTable table = tableRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException(
//                        "Table not found with id: " + id));
//
//        if (!table.getHotelId().equals(hotelId)) {
//            throw new RuntimeException(
//                    "Unauthorized: You cannot delete this table");
//        }
//
//        if (!"INACTIVE".equalsIgnoreCase(table.getStatus())) {
//            throw new RuntimeException(
//                    "Cannot delete table while it is " + table.getStatus());
//        }
//
//        tableRepository.delete(table);
//  }
}