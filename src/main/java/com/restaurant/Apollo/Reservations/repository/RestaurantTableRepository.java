package com.restaurant.Apollo.Reservations.repository;

import com.restaurant.Apollo.Reservations.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    
    List<RestaurantTable> findAllByIsActiveTrue();
    
    Optional<RestaurantTable> findByTableNumber(Integer tableNumber);
    
    List<RestaurantTable> findAllByCapacity(Integer capacity);
    
    List<RestaurantTable> findAllByCapacityAndIsActiveTrue(Integer capacity);
    
    boolean existsByTableNumber(Integer tableNumber);
}
