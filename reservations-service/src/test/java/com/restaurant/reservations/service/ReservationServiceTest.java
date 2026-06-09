package com.restaurant.reservations.service;

import com.restaurant.reservations.dto.CreateReservationRequest;
import com.restaurant.reservations.dto.ReservationResponse;
import com.restaurant.reservations.enums.ReservationStatus;
import com.restaurant.reservations.kafka.ReservationEventProducer;
import com.restaurant.reservations.model.Reservation;
import com.restaurant.reservations.model.RestaurantTable;
import com.restaurant.reservations.repository.ReservationRepository;
import com.restaurant.reservations.repository.RestaurantTableRepository;
import com.restaurant.shared.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Teste unitare pentru ReservationService.
 *
 * Acoperire:
 *  - #5  Slot CANCELLED poate fi re-rezervat (partial index activ-only)
 *  - #5  Slot CONFIRMED blochează rezervarea nouă
 *  - #5  DataIntegrityViolationException (race condition la insert) → 409
 *  - #6  Rezervare anonimă (fără user autentificat) → userId = null
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RestaurantTableRepository tableRepository;
    @Mock private ReservationEventProducer eventProducer;

    @InjectMocks
    private ReservationService reservationService;

    private RestaurantTable table;
    private CreateReservationRequest request;

    @BeforeEach
    void setUp() {
        table = new RestaurantTable();
        table.setId(1L);
        table.setTableNumber(5);
        table.setCapacity(4);
        table.setIsActive(true);

        request = new CreateReservationRequest();
        request.setTableId(1L);
        request.setCustomerName("Ion Popescu");
        request.setCustomerPhone("0712345678");
        request.setReservationDate(LocalDate.of(2026, 7, 15));
        request.setStartTime(LocalTime.of(12, 0));
        request.setPartySize(2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // #5 — Slot CANCELLED nu mai blochează o rezervare nouă
    // ------------------------------------------------------------------

    @Test
    void cancelledSlotDoesNotConflict_slotIsBookable() {
        // Given: findConflictingReservations filtrează pe CONFIRMED — nu returnează nimic
        // (simulează că există o rezervare CANCELLED, dar query-ul o ignoră)
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.findConflictingReservations(
                eq(1L), any(), any(), any()))
                .thenReturn(List.of()); // niciun conflict CONFIRMED

        Reservation saved = buildReservation(ReservationStatus.CONFIRMED);
        when(reservationRepository.save(any())).thenReturn(saved);

        // When
        ReservationResponse response = reservationService.createReservation(request);

        // Then: rezervarea a fost creată fără 409
        assertNotNull(response);
        verify(reservationRepository).save(any());
    }

    @Test
    void confirmedSlotBlocksNewBooking() {
        // Given: există o rezervare CONFIRMED pe același slot
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        Reservation existing = buildReservation(ReservationStatus.CONFIRMED);
        when(reservationRepository.findConflictingReservations(
                eq(1L), any(), any(), any()))
                .thenReturn(List.of(existing));

        // When / Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservationService.createReservation(request));
        assertTrue(ex.getMessage().contains("already reserved"));
        verify(reservationRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // #5 — Race condition: al doilea insert concurrent → 409 (nu 500)
    // ------------------------------------------------------------------

    @Test
    void dataIntegrityViolationMapsTo409Conflict() {
        // Given: ambele fire trec de preflight, al doilea insert violează partial index
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.findConflictingReservations(
                any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_active_reservation_slot"));

        // When
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.createReservation(request));

        // Then: 409, nu 500
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ------------------------------------------------------------------
    // #6 — Rezervare anonimă (fără JWT) — userId rămâne null
    // ------------------------------------------------------------------

    @Test
    void anonymousReservation_userIdIsNull() {
        // Given: niciun user autentificat în SecurityContext
        SecurityContextHolder.clearContext();

        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.findConflictingReservations(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        Reservation savedSlot = buildReservation(ReservationStatus.CONFIRMED);
        savedSlot.setUserId(null);
        when(reservationRepository.save(any())).thenReturn(savedSlot);

        // When
        ReservationResponse response = reservationService.createReservation(request);

        // Then: userId null — rezervare guest validă
        assertNull(response.getUserId());
    }

    @Test
    void authenticatedReservation_userIdIsSet() {
        // Given: user autentificat cu id=42
        UserPrincipal principal = new UserPrincipal(42L, "test@test.com", Set.of("ROLE_GUEST"));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.findConflictingReservations(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        Reservation savedSlot = buildReservation(ReservationStatus.CONFIRMED);
        savedSlot.setUserId(42L);
        when(reservationRepository.save(any())).thenReturn(savedSlot);

        // When
        ReservationResponse response = reservationService.createReservation(request);

        // Then
        assertEquals(42L, response.getUserId());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Reservation buildReservation(ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(99L);
        r.setTable(table);
        r.setCustomerName("Ion Popescu");
        r.setCustomerPhone("0712345678");
        r.setReservationDate(LocalDate.of(2026, 7, 15));
        r.setStartTime(LocalTime.of(12, 0));
        r.setEndTime(LocalTime.of(14, 0));
        r.setPartySize(2);
        r.setStatus(status);
        return r;
    }
}
