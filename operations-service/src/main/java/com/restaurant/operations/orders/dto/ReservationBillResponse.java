package com.restaurant.operations.orders.dto;

import java.util.List;

public record ReservationBillResponse(
        Long reservationId,
        List<OrderResponse> orders,
        List<AggregatedItemResponse> aggregatedItems,
        Double grandTotal
) {}
