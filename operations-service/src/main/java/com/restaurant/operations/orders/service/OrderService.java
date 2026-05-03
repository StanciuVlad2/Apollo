package com.restaurant.operations.orders.service;

import com.restaurant.operations.kafka.OrderEventProducer;
import com.restaurant.operations.menu.model.MenuItem;
import com.restaurant.operations.menu.model.RecipeIngredient;
import com.restaurant.operations.menu.service.MenuItemService;
import com.restaurant.operations.orders.dto.*;
import com.restaurant.operations.orders.enums.OrderStatus;
import com.restaurant.operations.orders.model.Order;
import com.restaurant.operations.orders.model.OrderItem;
import com.restaurant.operations.orders.repository.OrderRepository;
import com.restaurant.operations.stock.service.StockItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.operations.orders.dto.OrderReportData;
import com.restaurant.operations.orders.dto.TopItemData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemService menuItemService;
    private final StockItemService stockItemService;
    private final OrderEventProducer orderEventProducer;

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
        order.setCustomerEmail(request.customerEmail());
        order.setReservationId(request.reservationId());

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
     * When the status changes to COMPLETED, recipe ingredients are deducted from stock
     * and an order.completed Kafka event is published.
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

        if (target == OrderStatus.READY && order.getCustomerEmail() != null) {
            orderEventProducer.publishOrderReady(order.getId(), order.getTableId(), order.getCustomerEmail());
        }

        if (target == OrderStatus.COMPLETED) {
            deductStock(order);
            orderEventProducer.publishOrderCompleted(order.getId(), order.getUserId());
        }

        order.setStatus(target);
        return toResponse(orderRepository.save(order));
    }

    public OrderReportData generateReport(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        long completed = orders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long cancelled = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        double totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .flatMap(o -> o.getItems().stream())
                .mapToDouble(i -> i.getUnitPrice() * i.getQuantity())
                .sum();
        double avg = orders.isEmpty() ? 0.0 : Math.round((totalRevenue / orders.size()) * 100.0) / 100.0;

        Map<String, long[]> itemMap = new LinkedHashMap<>();
        Map<String, double[]> itemRevMap = new LinkedHashMap<>();
        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .flatMap(o -> o.getItems().stream())
                .forEach(i -> {
                    itemMap.computeIfAbsent(i.getMenuItemName(), k -> new long[]{0})[0] += i.getQuantity();
                    itemRevMap.computeIfAbsent(i.getMenuItemName(), k -> new double[]{0.0})[0] += i.getUnitPrice() * i.getQuantity();
                });

        List<TopItemData> topItems = itemMap.entrySet().stream()
                .map(e -> new TopItemData(e.getKey(), e.getValue()[0],
                        Math.round(itemRevMap.get(e.getKey())[0] * 100.0) / 100.0))
                .sorted((a, b) -> Long.compare(b.quantitySold(), a.quantitySold()))
                .toList();

        return new OrderReportData(orders.size(), completed, cancelled,
                Math.round(totalRevenue * 100.0) / 100.0, avg, topItems);
    }

    // -- Internal helpers ---------------------------------------------------

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
                    ". Allowed values: PENDING, READY, COMPLETED, CANCELLED");
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
                order.getCustomerEmail(),
                order.getReservationId(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses,
                total
        );
    }
}
