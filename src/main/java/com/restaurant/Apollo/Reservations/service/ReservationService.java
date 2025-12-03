package com.restaurant.Apollo.Reservations.service;

import com.restaurant.Apollo.UserManagement.model.User;
import com.restaurant.Apollo.Reservations.dto.*;
import com.restaurant.Apollo.Reservations.enums.ReservationStatus;
import com.restaurant.Apollo.Reservations.enums.TimeSlot;
import com.restaurant.Apollo.Reservations.model.Reservation;
import com.restaurant.Apollo.Reservations.model.RestaurantTable;
import com.restaurant.Apollo.Reservations.repository.ReservationRepository;
import com.restaurant.Apollo.Reservations.repository.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, User user) {
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

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setTable(table);
        reservation.setUser(user);
        reservation.setCustomerName(request.getCustomerName());
        reservation.setCustomerPhone(request.getCustomerPhone());
        reservation.setCustomerEmail(request.getCustomerEmail());
        reservation.setPartySize(request.getPartySize() != null ? request.getPartySize() : table.getCapacity());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setNotes(request.getNotes());

        Reservation savedReservation = reservationRepository.save(reservation);
        return mapToResponse(savedReservation);
    }

    public AvailabilityResponse checkAvailability(LocalDate date, Integer partySize) {
        List<RestaurantTable> allTables;
        
        if (partySize != null) {
            allTables = tableRepository.findAllByCapacityAndIsActiveTrue(partySize);
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

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, User user) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        // Check if user owns the reservation or is manager
        if (reservation.getUser() != null && !reservation.getUser().getId().equals(user.getId()) 
            && !user.getRoles().contains("MANAGER")) {
            throw new IllegalArgumentException("Not authorized to cancel this reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);
        return mapToResponse(updated);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setTableId(reservation.getTable().getId());
        response.setTableNumber(reservation.getTable().getTableNumber());
        response.setCustomerName(reservation.getCustomerName());
        response.setCustomerPhone(reservation.getCustomerPhone());
        response.setCustomerEmail(reservation.getCustomerEmail());
        response.setPartySize(reservation.getPartySize());
        response.setReservationDate(reservation.getReservationDate());
        response.setStartTime(reservation.getStartTime());
        response.setEndTime(reservation.getEndTime());
        response.setStatus(reservation.getStatus().name());
        response.setNotes(reservation.getNotes());
        return response;
    }
}
