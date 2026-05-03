package com.restaurant.reservations.controllers;

import com.restaurant.reservations.dto.*;
import com.restaurant.reservations.service.ReservationService;
import com.restaurant.shared.security.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer partySize) {
        AvailabilityResponse response = reservationService.checkAvailability(date, partySize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-reservations")
    public ResponseEntity<?> getMyReservations(
            @RequestParam(required = false, defaultValue = "false") boolean active) {
        Long userId = UserHolder.getCurrentUser().userId();

        List<ReservationResponse> reservations = active
            ? reservationService.getActiveReservationsForUser(userId)
            : reservationService.getUserReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        List<ReservationResponse> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) CancelReservationRequest request) {
        String cancelReason = request != null ? request.getCancelReason() : null;
        ReservationResponse response = reservationService.cancelReservation(id, cancelReason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active-for-table")
    @PreAuthorize("hasAnyRole('WAITER','CHEF','MANAGER','ADMIN')")
    public ResponseEntity<ReservationResponse> getActiveReservationForTable(
            @RequestParam Long tableId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getActiveReservationForTable(tableId, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
