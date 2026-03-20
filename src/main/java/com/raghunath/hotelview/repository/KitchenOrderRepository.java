package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.entity.KitchenOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface KitchenOrderRepository extends MongoRepository<KitchenOrder, String> {
    // Used by the Chef to see what needs to be cooked
    List<KitchenOrder> findByHotelIdAndStatusIn(String hotelId, List<String> statuses);
}