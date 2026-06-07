package com.restaurant.reservations.service;

import com.restaurant.reservations.dto.*;
import com.restaurant.reservations.enums.ReservationStatus;
import com.restaurant.reservations.enums.TimeSlot;
import com.restaurant.reservations.kafka.ReservationEvent;
import com.restaurant.reservations.kafka.ReservationEventProducer;
import com.restaurant.reservations.model.Reservation;
import com.restaurant.reservations.model.RestaurantTable;
import com.restaurant.reservations.repository.ReservationRepository;
import com.restaurant.reservations.repository.RestaurantTableRepository;
import com.restaurant.shared.security.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private ReservationEventProducer reservationEventProducer;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        // Validate table exists
        RestaurantTable table = tableRepository.findById(request.getTableId())
            .orElseThrow(() -> new IllegalArgumentException("Table not found: " + request.getTableId()));

        if (!table.getIsActive()) {
            throw new IllegalArgumentException("Table is not active");
        }

        // Validate time slot
        TimeSlot timeSlot;
        try {
            timeSlot = TimeSlot.fromStartTime(request.getStartTime());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid time slot. Must be one of: 10:00, 12:00, 14:00, 16:00, 18:00, 20:00");
        }

        LocalTime endTime = timeSlot.getEndTime();

        // Check for conflicts
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
            request.getTableId(),
            request.getReservationDate(),
            request.getStartTime(),
            endTime
        );

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Table is already reserved for this time slot");
        }

        // Validate party size
        if (request.getPartySize() != null && request.getPartySize() > table.getCapacity()) {
            throw new IllegalArgumentException("Party size exceeds table capacity");
        }

        // Resolve the current user id (null if unauthenticated)
        Long userId = null;
        try {
            userId = UserHolder.getCurrentUser().userId();
        } catch (IllegalStateException ignored) {
            // unauthenticated guest reservation — userId stays null
        }

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setTable(table);
        reservation.setUserId(userId);
        reservation.setCustomerName(request.getCustomerName());
        reservation.setCustomerPhone(request.getCustomerPhone());
        reservation.setCustomerEmail(request.getCustomerEmail());
        reservation.setPartySize(request.getPartySize() != null ? request.getPartySize() : table.getCapacity());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setNotes(request.getNotes());

        Reservation savedReservation;
        try {
            savedReservation = reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Table is already reserved for this time slot");
        }

        if (savedReservation.getCustomerEmail() != null) {
            reservationEventProducer.publish(new ReservationEvent(
                "CONFIRMED",
                savedReservation.getCustomerEmail(),
                savedReservation.getCustomerName(),
                savedReservation.getPartySize(),
                savedReservation.getReservationDate(),
                savedReservation.getStartTime(),
                savedReservation.getEndTime(),
                null
            ));
        }

        return mapToResponse(savedReservation);
    }

    public AvailabilityResponse checkAvailability(LocalDate date, Integer partySize) {
        List<RestaurantTable> allTables;

        if (partySize != null) {
            allTables = tableRepository.findAllByCapacityGreaterThanEqualAndIsActiveTrue(partySize);
        } else {
            allTables = tableRepository.findAllByIsActiveTrue();
        }

        List<AvailabilityResponse.TimeSlotAvailability> availableSlots = new ArrayList<>();

        for (TimeSlot slot : TimeSlot.values()) {
            List<RestaurantTable> reservedTables = reservationRepository.findReservedTablesForTimeSlot(
                date, slot.getStartTime()
            );

            List<AvailabilityResponse.TableAvailability> tableAvailabilities = allTables.stream()
                .map(table -> {
                    boolean isReserved = reservedTables.stream()
                        .anyMatch(reserved -> reserved.getId().equals(table.getId()));

                    return new AvailabilityResponse.TableAvailability(
                        table.getId(),
                        table.getTableNumber(),
                        table.getCapacity(),
                        table.getXPosition(),
                        table.getYPosition(),
                        table.getWidth(),
                        table.getHeight(),
                        !isReserved
                    );
                })
                .collect(Collectors.toList());

            // Only include time slot if at least one table is available
            boolean hasAvailableTables = tableAvailabilities.stream()
                .anyMatch(AvailabilityResponse.TableAvailability::getAvailable);

            if (hasAvailableTables) {
                availableSlots.add(new AvailabilityResponse.TimeSlotAvailability(
                    slot.name(),
                    slot.getStartTime().toString(),
                    slot.getEndTime().toString(),
                    tableAvailabilities
                ));
            }
        }

        return new AvailabilityResponse(availableSlots);
    }

    public List<ReservationResponse> getUserReservations(Long userId) {
        return reservationRepository.findAllByUserId(userId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<ReservationResponse> getActiveReservationsForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime today = LocalTime.now();
        log.info("Finding active reservations for user {} at {} on {}", userId, today, now.toLocalDate());
        System.out.println("Finding active reservations for user " + userId + " at " + today + " on " + now.toLocalDate());
        return reservationRepository.findActiveNowForUser(userId, now.toLocalDate(), today)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, String cancelReason) {
        Long currentUserId = UserHolder.getCurrentUser().userId();
        boolean isManager = UserHolder.getCurrentUser().roles().contains("ROLE_MANAGER")
                || UserHolder.getCurrentUser().roles().contains("ROLE_ADMIN");

        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        // Check if user owns the reservation or is manager/admin
        if (!isManager && (reservation.getUserId() == null || !reservation.getUserId().equals(currentUserId))) {
            throw new IllegalArgumentException("Not authorized to cancel this reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelReason(cancelReason);
        Reservation updated = reservationRepository.save(reservation);

        if (reservation.getCustomerEmail() != null) {
            reservationEventProducer.publish(new ReservationEvent(
                "CANCELLED",
                reservation.getCustomerEmail(),
                reservation.getCustomerName(),
                reservation.getPartySize(),
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                cancelReason
            ));
        }

        return mapToResponse(updated);
    }

    public Optional<ReservationResponse> getActiveReservationForTable(Long tableId, LocalDate date) {
        return reservationRepository.findByTableIdAndReservationDateAndStatus(
                tableId, date, ReservationStatus.CONFIRMED)
            .map(this::mapToResponse);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setTableId(reservation.getTable().getId());
        response.setTableNumber(reservation.getTable().getTableNumber());
        response.setUserId(reservation.getUserId());
        response.setCustomerName(reservation.getCustomerName());
        response.setCustomerPhone(reservation.getCustomerPhone());
        response.setCustomerEmail(reservation.getCustomerEmail());
        response.setPartySize(reservation.getPartySize());
        response.setReservationDate(reservation.getReservationDate());
        response.setStartTime(reservation.getStartTime());
        response.setEndTime(reservation.getEndTime());
        response.setStatus(reservation.getStatus().name());
        response.setNotes(reservation.getNotes());
        response.setCancelReason(reservation.getCancelReason());
        return response;
    }
}
