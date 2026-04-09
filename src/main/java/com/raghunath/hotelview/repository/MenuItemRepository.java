package com.raghunath.hotelview.repository;

import com.raghunath.hotelview.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {

    List<MenuItem> findByHotelIdAndIsApprovedTrue(String hotelId);


    @Query("{ 'hotelId': ?0, 'category': ?1, 'isApproved': true }")
    List<MenuItem> findByHotelIdAndCategoryAndIsApprovedTrue(String hotelId, String category);


    @Query("{ 'hotelId': ?0, '_id': ?1 }")
    Optional<MenuItem> findByHotelIdAndId(String hotelId, String id);


    @Query("{ 'hotelId': ?0, 'name': ?1 }")
    Optional<MenuItem> findByHotelIdAndName(String hotelId, String name);


    Page<MenuItem> findByHotelIdAndIsApprovedTrue(String hotelId, Pageable pageable);


    @Query("{ 'hotelId': ?0, 'isApproved': true, $text: { $search: ?1 } }")
    List<MenuItem> findByHotelIdAndSearchQuery(String hotelId, String query);


    @Query(value = "{ 'hotelId': ?0, 'isApproved': true }",
            fields = "{ 'name': 1, 'price': 1, 'shortCode': 1, 'category': 1, 'isAvailable': 1, 'isVeg': 1 }")
    List<MenuItem> findAllByHotelIdForLocalCache(String hotelId);


    @Query(value = "{ 'hotelId': ?0, 'isApproved': true }",
            fields = "{ 'id': 1, 'category': 1, 'name': 1, 'shortCode': 1, 'description': 1, 'price': 1, 'isVeg': 1, 'isAvailable': 1, 'imageUrl': 1 }")
    List<MenuItem> findAllByHotelIdForCache(String hotelId);


    @Query(value = "{ 'hotelId': ?0, 'isApproved': true, $text: { $search: ?1 } }",
            fields = "{ 'id': 1, 'category': 1, 'name': 1, 'shortCode': 1, 'description': 1, 'price': 1, 'isVeg': 1, 'isAvailable': 1, 'imageUrl': 1 }")
    List<MenuItem> searchByHotelIdWithProjection(String hotelId, String query);


    Optional<MenuItem> findTopByHotelIdOrderByUpdatedAtDesc(String hotelId);


    long countByHotelId(String hotelId);


    List<MenuItem> findByHotelIdAndUpdatedAtGreaterThan(String hotelId, LocalDateTime lastSync);
}