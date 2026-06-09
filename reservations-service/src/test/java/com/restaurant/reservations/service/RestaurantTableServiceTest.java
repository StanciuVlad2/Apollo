package com.restaurant.reservations.service;

import com.restaurant.reservations.enums.ReservationStatus;
import com.restaurant.reservations.model.RestaurantTable;
import com.restaurant.reservations.repository.ReservationRepository;
import com.restaurant.reservations.repository.RestaurantTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste unitare pentru RestaurantTableService.
 *
 * Acoperire:
 *  - M3  CONFIRMED → 409 (comportament existent păstrat)
 *  - M3  CANCELLED/COMPLETED/NO_SHOW → soft-delete, nu FK violation 500
 *  - M3  Fără rezervări → hard-delete
 *  - M3  Masă inexistentă → IllegalArgumentException
 */
@ExtendWith(MockitoExtension.class)
class RestaurantTableServiceTest {

    @Mock private RestaurantTableRepository tableRepository;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks
    private RestaurantTableService tableService;

    @Captor
    private ArgumentCaptor<RestaurantTable> tableCaptor;

    private RestaurantTable table;

    @BeforeEach
    void setUp() {
        table = new RestaurantTable();
        table.setId(1L);
        table.setTableNumber(3);
        table.setCapacity(4);
        table.setIsActive(true);
    }

    // ------------------------------------------------------------------
    // M3 — CONFIRMED blochează ștergerea cu 409
    // ------------------------------------------------------------------

    @Test
    void deleteTable_withConfirmedReservation_throws409() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.existsByTableIdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tableService.deleteTable(1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(tableRepository, never()).deleteById(any());
        verify(tableRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // M3 — Rezervări istorice → soft-delete (nu FK violation)
    // ------------------------------------------------------------------

    @Test
    void deleteTable_withCancelledReservations_softDeletes() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.existsByTableIdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(false);
        when(reservationRepository.existsByTableId(1L)).thenReturn(true);

        tableService.deleteTable(1L);

        // Trebuie să salveze cu isActive=false, nu să șteargă
        verify(tableRepository, never()).deleteById(any());
        verify(tableRepository).save(tableCaptor.capture());
        assertFalse(tableCaptor.getValue().getIsActive());
    }

    @Test
    void deleteTable_withCompletedReservations_softDeletes() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.existsByTableIdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(false);
        when(reservationRepository.existsByTableId(1L)).thenReturn(true);

        tableService.deleteTable(1L);

        verify(tableRepository, never()).deleteById(any());
        verify(tableRepository).save(tableCaptor.capture());
        assertFalse(tableCaptor.getValue().getIsActive());
    }

    // ------------------------------------------------------------------
    // M3 — Fără rezervări → hard-delete normal
    // ------------------------------------------------------------------

    @Test
    void deleteTable_withNoReservations_hardDeletes() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.existsByTableIdAndStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(false);
        when(reservationRepository.existsByTableId(1L)).thenReturn(false);

        tableService.deleteTable(1L);

        verify(tableRepository).deleteById(1L);
        verify(tableRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // M3 — Masă inexistentă
    // ------------------------------------------------------------------

    @Test
    void deleteTable_tableNotFound_throwsIllegalArgument() {
        when(tableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> tableService.deleteTable(99L));

        verify(tableRepository, never()).deleteById(any());
        verify(tableRepository, never()).save(any());
    }
}
