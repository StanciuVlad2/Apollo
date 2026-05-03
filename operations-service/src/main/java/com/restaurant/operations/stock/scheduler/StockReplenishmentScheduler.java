package com.restaurant.operations.stock.scheduler;

import com.restaurant.operations.stock.dto.ReplenishmentEventDto;
import com.restaurant.operations.stock.model.StockItem;
import com.restaurant.operations.stock.repository.StockItemRepository;
import com.restaurant.operations.stock.sse.StockSseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Scheduled task that automatically replenishes low-stock items.
 * Runs daily at 01:00 AM (1 AM).
 *
 * For each item where quantity < minimumThreshold,
 * sets quantity = minimumThreshold * 2.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockReplenishmentScheduler {

    private final StockItemRepository stockItemRepository;
    private final StockSseEmitterService sseEmitterService;

    /**
     * Auto-replenish low-stock items.
     * Cron: 0 0 1 * * * → Every day at 01:00 AM
     */
    @Scheduled(cron = "0 * * * * *")
    public void replenishLowStockItems() {
        log.info("Starting scheduled stock replenishment task...");

        try {
            // Fetch all stock items
            List<StockItem> allItems = StreamSupport.stream(
                    stockItemRepository.findAll().spliterator(),
                    false
            ).toList();

            List<StockItem> lowStockItems = allItems.stream()
                    .filter(item -> item.getMinimumThreshold() != null
                            && item.getQuantity() < item.getMinimumThreshold())
                    .toList();

            if (lowStockItems.isEmpty()) {
                log.info("No low-stock items found. Replenishment task completed.");
                return;
            }

            log.info("Found {} low-stock items. Starting replenishment...", lowStockItems.size());

            // Replenish each low-stock item
            for (StockItem item : lowStockItems) {
                Double newQuantity = item.getMinimumThreshold() * 2;
                Double oldQuantity = item.getQuantity();

                item.setQuantity(newQuantity);
                stockItemRepository.save(item);

                log.info(
                        "Replenished item '{}': {} {} → {} {} (min threshold: {})",
                        item.getName(),
                        oldQuantity,
                        item.getUnit(),
                        newQuantity,
                        item.getUnit(),
                        item.getMinimumThreshold()
                );
            }

            // Broadcast replenishment event to all connected SSE clients
            sseEmitterService.broadcast(new ReplenishmentEventDto(
                    lowStockItems.size(),
                    lowStockItems.stream().map(StockItem::getName).toList()
            ));

            log.info("Stock replenishment task completed successfully. {} items updated.",
                    lowStockItems.size());

        } catch (Exception e) {
            log.error("Error during stock replenishment task", e);
        }
    }
}


