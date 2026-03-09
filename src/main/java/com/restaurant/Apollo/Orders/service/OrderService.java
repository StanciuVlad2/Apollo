package com.restaurant.Apollo.Orders.service;

import com.restaurant.Apollo.Menu.model.MenuItem;
import com.restaurant.Apollo.Menu.model.RecipeIngredient;
import com.restaurant.Apollo.Menu.service.MenuItemService;
import com.restaurant.Apollo.Orders.dto.*;
import com.restaurant.Apollo.Orders.enums.OrderStatus;
import com.restaurant.Apollo.Orders.model.Order;
import com.restaurant.Apollo.Orders.model.OrderItem;
import com.restaurant.Apollo.Orders.repository.OrderRepository;
import com.restaurant.Apollo.Stock.service.StockItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemService menuItemService;
    private final StockItemService stockItemService;

    public List<OrderResponse> getAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getByStatus(String status) {
        OrderStatus orderStatus = parseStatus(status);
        return orderRepository.findByStatus(orderStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getById(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, Long userId) {
        Order order = Order.builder()
                .tableId(request.tableId())
                .userId(userId)
                .status(OrderStatus.PENDING)
                .notes(request.notes())
                .items(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.items()) {
            MenuItem menuItem = menuItemService.getRawById(itemReq.menuItemId());
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .menuItemId(menuItem.getId())
                    .menuItemName(menuItem.getName())
                    .quantity(itemReq.quantity())
                    .unitPrice(menuItem.getPrice())
                    .build();
            orderItems.add(orderItem);
        }
        savedOrder.getItems().addAll(orderItems);
        return toResponse(orderRepository.save(savedOrder));
    }

    /**
     * Transitions an order to a new status.
     * When the status changes to COMPLETED, recipe ingredients are deducted from stock.
     */
    @Transactional
    public OrderResponse updateStatus(Long id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));

        OrderStatus target = parseStatus(newStatus);

        if (order.getStatus() == target) {
            return toResponse(order);
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot change status of a " + order.getStatus().name().toLowerCase() + " order.");
        }

        if (target == OrderStatus.COMPLETED) {
            deductStock(order);
        }

        order.setStatus(target);
        return toResponse(orderRepository.save(order));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * For each order item, fetches the recipe from Elasticsearch and deducts
     * the required ingredient quantities from stock.
     */
    private void deductStock(Order order) {
        for (OrderItem orderItem : order.getItems()) {
            MenuItem menuItem;
            try {
                menuItem = menuItemService.getRawById(orderItem.getMenuItemId());
            } catch (NoSuchElementException e) {
                log.warn("Menu item {} not found in ES – skipping stock deduction", orderItem.getMenuItemId());
                continue;
            }

            for (RecipeIngredient ingredient : menuItem.getRecipe()) {
                double totalNeeded = ingredient.getQuantity() * orderItem.getQuantity();
                try {
                    stockItemService.deduct(ingredient.getIngredientName(), totalNeeded, ingredient.getUnit());
                    log.info("Deducted {} {} of {} for order {}",
                            totalNeeded, ingredient.getUnit(),
                            ingredient.getIngredientName(), order.getId());
                } catch (NoSuchElementException e) {
                    log.warn("Stock item '{}' not found – skipping deduction", ingredient.getIngredientName());
                }
            }
        }
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status +
                    ". Allowed values: PENDING, COMPLETED, CANCELLED");
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getMenuItemId(),
                        i.getMenuItemName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice() * i.getQuantity()
                ))
                .toList();

        double total = itemResponses.stream()
                .mapToDouble(OrderItemResponse::subtotal)
                .sum();

        return new OrderResponse(
                order.getId(),
                order.getTableId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses,
                total
        );
    }
}
