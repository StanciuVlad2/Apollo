package com.restaurant.Apollo.Reservations.repository;

import com.restaurant.Apollo.Reservations.enums.ReservationStatus;
import com.restaurant.Apollo.Reservations.model.Reservation;
import com.restaurant.Apollo.Reservations.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findAllByReservationDateAndStatus(LocalDate date, ReservationStatus status);
    
    List<Reservation> findAllByTableAndReservationDateAndStatus(
        RestaurantTable table, LocalDate date, ReservationStatus status
    );
    
    @Query("SELECT r FROM Reservation r WHERE r.table.id = :tableId " +
           "AND r.reservationDate = :date " +
           "AND r.status = 'CONFIRMED' " +
           "AND ((r.startTime < :endTime AND r.endTime > :startTime))")
    List<Reservation> findConflictingReservations(
        @Param("tableId") Long tableId,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    @Query("SELECT DISTINCT r.table FROM Reservation r WHERE r.reservationDate = :date " +
           "AND r.startTime = :startTime AND r.status = 'CONFIRMED'")
    List<RestaurantTable> findReservedTablesForTimeSlot(
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime
    );
    
    List<Reservation> findAllByUserId(Long userId);
}
