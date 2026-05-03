package com.restaurant.operations.stock.scheduler;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReplenishmentSchedulerTest {

    @Mock
    private StockItemRepository stockItemRepository;

    @InjectMocks
    private StockReplenishmentScheduler scheduler;

    @Captor
    private ArgumentCaptor<StockItem> itemCaptor;

    private List<StockItem> mockItems;

    @BeforeEach
    void setUp() {
        mockItems = List.of(
                // Low stock: quantity = 5, minimum = 10
                StockItem.builder()
                        .id("1")
                        .name("flour")
                        .quantity(5.0)
                        .unit("kg")
                        .minimumThreshold(10.0)
                        .type(StockType.SOLID)
                        .build(),
                // Low stock: quantity = 50, minimum = 100
                StockItem.builder()
                        .id("2")
                        .name("oil")
                        .quantity(50.0)
                        .unit("liters")
                        .minimumThreshold(100.0)
                        .type(StockType.LIQUID)
                        .build(),
                // Normal stock: quantity = 20, minimum = 10 (no replenishment needed)
                StockItem.builder()
                        .id("3")
                        .name("salt")
                        .quantity(20.0)
                        .unit("kg")
                        .minimumThreshold(10.0)
                        .type(StockType.SOLID)
                        .build(),
                // No minimum threshold set
                StockItem.builder()
                        .id("4")
                        .name("sugar")
                        .quantity(5.0)
                        .unit("kg")
                        .minimumThreshold(null)
                        .type(StockType.SOLID)
                        .build()
        );
    }

    @Test
    void shouldReplenishOnlyLowStockItems() {
        // Given
        when(stockItemRepository.findAll()).thenReturn(mockItems);
        when(stockItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        scheduler.replenishLowStockItems();

        // Then: only 2 items should be saved (flour and oil)
        verify(stockItemRepository, times(2)).save(itemCaptor.capture());

        List<StockItem> savedItems = itemCaptor.getAllValues();
        assertEquals(2, savedItems.size());

        // Check flour: 5 -> 20 (10 * 2)
        StockItem flour = savedItems.get(0);
        assertEquals("flour", flour.getName());
        assertEquals(20.0, flour.getQuantity());

        // Check oil: 50 -> 200 (100 * 2)
        StockItem oil = savedItems.get(1);
        assertEquals("oil", oil.getName());
        assertEquals(200.0, oil.getQuantity());
    }

    @Test
    void shouldNotReplenishItemsWithNormalStock() {
        // Given: only salt (normal stock)
        List<StockItem> normalStock = List.of(mockItems.get(2));
        when(stockItemRepository.findAll()).thenReturn(normalStock);

        // When
        scheduler.replenishLowStockItems();

        // Then: no items should be saved
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void shouldNotReplenishItemsWithoutMinimumThreshold() {
        // Given: only sugar (no minimum threshold)
        List<StockItem> noThreshold = List.of(mockItems.get(3));
        when(stockItemRepository.findAll()).thenReturn(noThreshold);

        // When
        scheduler.replenishLowStockItems();

        // Then: no items should be saved
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void shouldHandleEmptyStockList() {
        // Given
        when(stockItemRepository.findAll()).thenReturn(List.of());

        // When - Should not throw exception
        assertDoesNotThrow(() -> scheduler.replenishLowStockItems());

        // Then: no items should be saved
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void shouldHandleExceptionGracefully() {
        // Given: repository throws exception
        when(stockItemRepository.findAll()).thenThrow(
                new RuntimeException("Database connection failed"));

        // When - Should not throw exception
        assertDoesNotThrow(() -> scheduler.replenishLowStockItems());

        // Then: error should be logged (we can't directly verify log output in unit test)
    }

    @Test
    void shouldSetQuantityToExactlyTwiceTheMinimumThreshold() {
        // Given
        List<StockItem> singleItem = List.of(
                StockItem.builder()
                        .id("test")
                        .name("test-item")
                        .quantity(1.5)
                        .unit("kg")
                        .minimumThreshold(7.5)
                        .type(StockType.SOLID)
                        .build()
        );
        when(stockItemRepository.findAll()).thenReturn(singleItem);
        when(stockItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        scheduler.replenishLowStockItems();

        // Then: quantity should be exactly 15.0 (7.5 * 2)
        verify(stockItemRepository, times(1)).save(itemCaptor.capture());
        assertEquals(15.0, itemCaptor.getValue().getQuantity());
    }
}

