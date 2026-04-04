package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.entity.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByUsername(String username);
    List<Employee> findAllByHotelId(String hotelId);
    boolean existsByUsername(String username);

    Long countByHotelIdAndIsActive(String hotelId, boolean b);
}