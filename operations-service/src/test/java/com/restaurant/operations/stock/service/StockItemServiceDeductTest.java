package com.restaurant.operations.stock.service;

import com.restaurant.operations.stock.model.StockItem;
import com.restaurant.operations.stock.model.StockType;
import com.restaurant.operations.stock.repository.StockItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste unitare pentru StockItemService.deduct().
 *
 * Acoperire:
 *  - M5  Guard împotriva stocului negativ — deduct() aruncă IllegalStateException
 *  - M5  Deducție normală cu stoc suficient
 *  - M5  Deducție cu conversie de unități (rețetă în g, stoc în kg)
 *  - M5  Deducție care aduce stocul exact la 0 — permisă
 */
@ExtendWith(MockitoExtension.class)
class StockItemServiceDeductTest {

    @Mock private StockItemRepository stockItemRepository;

    @InjectMocks
    private StockItemService stockItemService;

    @Captor
    private ArgumentCaptor<StockItem> itemCaptor;

    private StockItem flourItem;

    @BeforeEach
    void setUp() {
        flourItem = StockItem.builder()
                .id("stock-1")
                .name("faina")
                .quantity(1.0)   // 1 kg în stoc
                .unit("kg")
                .type(StockType.SOLID)
                .build();
    }

    // ------------------------------------------------------------------
    // M5 — Guard: deduct() nu permite stoc negativ
    // ------------------------------------------------------------------

    @Test
    void deduct_wouldGoNegative_throwsIllegalState() {
        // Given: 1 kg în stoc, rețeta cere 1.5 kg
        when(stockItemRepository.findByNameIgnoreCase("faina"))
                .thenReturn(Optional.of(flourItem));

        // When / Then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> stockItemService.deduct("faina", 1.5, "kg"));

        assertTrue(ex.getMessage().contains("faina"));
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void deduct_exactlyDepletes_permittedAndSavesZero() {
        // Given: 1 kg în stoc, rețeta cere exact 1 kg
        when(stockItemRepository.findByNameIgnoreCase("faina"))
                .thenReturn(Optional.of(flourItem));
        when(stockItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        stockItemService.deduct("faina", 1.0, "kg");

        // Then: stoc = 0, nu negativ
        verify(stockItemRepository).save(itemCaptor.capture());
        assertEquals(0.0, itemCaptor.getValue().getQuantity());
    }

    // ------------------------------------------------------------------
    // M5 — Deducție normală cu stoc suficient
    // ------------------------------------------------------------------

    @Test
    void deduct_sufficientStock_reducesQuantity() {
        // Given: 1 kg în stoc, rețeta cere 300 g
        when(stockItemRepository.findByNameIgnoreCase("faina"))
                .thenReturn(Optional.of(flourItem));
        when(stockItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When: 300g = 0.3 kg din rețetă, stocul e în kg
        stockItemService.deduct("faina", 300.0, "g");

        // Then: 1.0 - 0.3 = 0.7 kg
        verify(stockItemRepository).save(itemCaptor.capture());
        assertEquals(0.7, itemCaptor.getValue().getQuantity(), 0.001);
    }

    // ------------------------------------------------------------------
    // M5 — Conversie unități: rețetă în g, stoc în kg
    // ------------------------------------------------------------------

    @Test
    void deduct_unitConversion_gToKg_correct() {
        // Given: 0.5 kg în stoc, rețeta cere 200 g
        flourItem.setQuantity(0.5);
        when(stockItemRepository.findByNameIgnoreCase("faina"))
                .thenReturn(Optional.of(flourItem));
        when(stockItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        stockItemService.deduct("faina", 200.0, "g");

        // Then: 0.5 - 0.2 = 0.3 kg
        verify(stockItemRepository).save(itemCaptor.capture());
        assertEquals(0.3, itemCaptor.getValue().getQuantity(), 0.001);
    }

    @Test
    void deduct_unitConversion_wouldGoNegative_throws() {
        // Given: 0.1 kg în stoc (100 g), rețeta cere 200 g
        flourItem.setQuantity(0.1);
        when(stockItemRepository.findByNameIgnoreCase("faina"))
                .thenReturn(Optional.of(flourItem));

        assertThrows(IllegalStateException.class,
                () -> stockItemService.deduct("faina", 200.0, "g"));

        verify(stockItemRepository, never()).save(any());
    }
}
