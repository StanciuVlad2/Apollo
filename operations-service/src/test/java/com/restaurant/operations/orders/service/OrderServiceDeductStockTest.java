package com.restaurant.operations.orders.service;

import com.restaurant.operations.kafka.OrderEventProducer;
import com.restaurant.operations.menu.model.MenuItem;
import com.restaurant.operations.menu.model.RecipeIngredient;
import com.restaurant.operations.menu.service.MenuItemService;
import com.restaurant.operations.orders.enums.OrderStatus;
import com.restaurant.operations.orders.model.Order;
import com.restaurant.operations.orders.model.OrderItem;
import com.restaurant.operations.orders.repository.OrderRepository;
import com.restaurant.operations.stock.service.StockItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Teste unitare pentru OrderService.deductStock() (invocată prin updateStatus → COMPLETED).
 *
 * Acoperire:
 *  - M5  Verificare cumulativă: același ingredient în două items separate
 *        → fiecare trece individual, dar suma depășește stocul → excepție
 *  - M5  Verificare cumulativă: suma e ok → ambele items sunt deduse
 *  - M5  Un singur item: preflight și deducție corecte
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceDeductStockTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MenuItemService menuItemService;
    @Mock private StockItemService stockItemService;
    @Mock private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private MenuItem pizzaMenuItem;
    private MenuItem pastaMenuItem;

    @BeforeEach
    void setUp() {
        // Menu item Pizza: 300g faina
        pizzaMenuItem = MenuItem.builder()
                .id("menu-pizza")
                .name("Pizza Margherita")
                .recipe(List.of(
                        RecipeIngredient.builder()
                                .ingredientName("faina")
                                .quantity(300.0)
                                .unit("g")
                                .build()
                ))
                .build();

        // Menu item Pasta: 250g faina
        pastaMenuItem = MenuItem.builder()
                .id("menu-pasta")
                .name("Pasta Carbonara")
                .recipe(List.of(
                        RecipeIngredient.builder()
                                .ingredientName("faina")
                                .quantity(250.0)
                                .unit("g")
                                .build()
                ))
                .build();

        // Comandă cu un item Pizza (cantitate 1) și un item Pasta (cantitate 1)
        // Total faina necesara: 300g + 250g = 550g
        OrderItem pizzaItem = OrderItem.builder()
                .menuItemId("menu-pizza")
                .menuItemName("Pizza Margherita")
                .quantity(1)
                .unitPrice(25.0)
                .build();

        OrderItem pastaItem = OrderItem.builder()
                .menuItemId("menu-pasta")
                .menuItemName("Pasta Carbonara")
                .quantity(1)
                .unitPrice(20.0)
                .build();

        order = Order.builder()
                .id(1L)
                .status(OrderStatus.BILLED)
                .items(List.of(pizzaItem, pastaItem))
                .build();
    }

    // ------------------------------------------------------------------
    // M5 — Verificare cumulativă: suma depășește stocul → niciun deduct
    // ------------------------------------------------------------------

    @Test
    void cumulativeCheck_samIngredientInTwoItems_failsWhenSumExceedsStock() {
        // Given: stoc insuficient pentru suma celor două items (550g total)
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.getRawById("menu-pizza")).thenReturn(pizzaMenuItem);
        when(menuItemService.getRawById("menu-pasta")).thenReturn(pastaMenuItem);

        // hasSufficientStock pentru 550g faina returnează false (stoc = 400g)
        when(stockItemService.hasSufficientStock("faina", 550.0, "g")).thenReturn(false);

        // When / Then
        assertThrows(IllegalStateException.class,
                () -> orderService.updateStatus(1L, "COMPLETED"));

        // Niciun deduct nu trebuie apelat
        verify(stockItemService, never()).deduct(any(), anyDouble(), any());
    }

    @Test
    void cumulativeCheck_samIngredientInTwoItems_passesWhenSumFits() {
        // Given: stoc suficient pentru 550g (stoc = 800g)
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.getRawById("menu-pizza")).thenReturn(pizzaMenuItem);
        when(menuItemService.getRawById("menu-pasta")).thenReturn(pastaMenuItem);
        when(stockItemService.hasSufficientStock("faina", 550.0, "g")).thenReturn(true);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        orderService.updateStatus(1L, "COMPLETED");

        // Then: deductiile se executa pentru fiecare item individual
        verify(stockItemService).deduct("faina", 300.0, "g"); // pizza
        verify(stockItemService).deduct("faina", 250.0, "g"); // pasta
    }

    // ------------------------------------------------------------------
    // M5 — Ingredient din un singur item: preflight și deducție corecte
    // ------------------------------------------------------------------

    @Test
    void singleItem_sufficientStock_deductsOnce() {
        // Comandă cu un singur item
        OrderItem singleItem = OrderItem.builder()
                .menuItemId("menu-pizza")
                .menuItemName("Pizza Margherita")
                .quantity(2)   // 2 pizza = 600g faina
                .unitPrice(25.0)
                .build();
        Order singleOrder = Order.builder()
                .id(2L)
                .status(OrderStatus.BILLED)
                .items(List.of(singleItem))
                .build();

        when(orderRepository.findById(2L)).thenReturn(Optional.of(singleOrder));
        when(menuItemService.getRawById("menu-pizza")).thenReturn(pizzaMenuItem);
        when(stockItemService.hasSufficientStock("faina", 600.0, "g")).thenReturn(true);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        orderService.updateStatus(2L, "COMPLETED");

        // Then: un singur deduct cu cantitatea corectă
        verify(stockItemService, times(1)).deduct("faina", 600.0, "g");
    }
}
