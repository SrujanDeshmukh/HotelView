package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.entity.ExternalOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ExternalOrderRepository extends MongoRepository<ExternalOrder, String> {

    // ADD THESE TWO LINES:
    boolean existsByExternalOrderId(String externalOrderId);

    Optional<ExternalOrder> findByExternalOrderId(String externalOrderId);
}