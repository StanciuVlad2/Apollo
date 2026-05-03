package com.restaurant.operations.stock.dto;

import java.util.List;

/**
 * Event payload sent to clients when stock replenishment completes.
 *
 * @param count number of items replenished
 * @param itemNames list of replenished item names
 */
public record ReplenishmentEventDto(int count, List<String> itemNames) {}
