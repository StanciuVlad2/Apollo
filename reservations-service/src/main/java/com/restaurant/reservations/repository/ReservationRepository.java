package com.restaurant.reservations.repository;

import com.restaurant.reservations.enums.ReservationStatus;
import com.restaurant.reservations.model.Reservation;
import com.restaurant.reservations.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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

    List<Reservation> findByReservationDateBetween(LocalDate from, LocalDate to);

    Optional<Reservation> findByTableIdAndReservationDateAndStatus(
        Long tableId, LocalDate reservationDate, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId " +
           "AND r.reservationDate = :today " +
           "AND r.startTime <= :now " +
           "AND r.endTime > :now " +
           "AND r.status = 'CONFIRMED'")
    List<Reservation> findActiveNowForUser(
        @Param("userId") Long userId,
        @Param("today") LocalDate today,
        @Param("now") LocalTime now
    );
}
