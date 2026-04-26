package com.restaurant.operations.stock.service;

import com.restaurant.operations.stock.dto.StockItemRequest;
import com.restaurant.operations.stock.dto.StockItemResponse;
import com.restaurant.operations.stock.model.StockItem;
import com.restaurant.operations.stock.model.StockType;
import com.restaurant.operations.stock.repository.StockItemRepository;
import com.restaurant.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class StockItemService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StockItemRepository stockItemRepository;

    public PageResponse<StockItemResponse> getAllPaged(String search, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize);

        Page<StockItem> result;
        if (search != null && !search.isBlank()) {
            result = stockItemRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            result = stockItemRepository.findAll(pageable);
        }

        return PageResponse.from(result, this::toResponse);
    }

    public List<StockItemResponse> getAll() {
        return StreamSupport.stream(stockItemRepository.findAll().spliterator(), false)
                .map(this::toResponse)
                .toList();
    }

    public StockItemResponse getById(String id) {
        return stockItemRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Stock item not found: " + id));
    }

    public List<StockItemResponse> search(String name) {
        return stockItemRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<StockItemResponse> getLowStock() {
        return StreamSupport.stream(stockItemRepository.findAll().spliterator(), false)
                .filter(item -> item.getMinimumThreshold() != null
                        && item.getQuantity() < item.getMinimumThreshold())
                .map(this::toResponse)
                .toList();
    }

    public StockItemResponse create(StockItemRequest request) {
        if (stockItemRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Stock item already exists: " + request.name());
        }
        StockItem item = StockItem.builder()
                .name(request.name().toLowerCase().trim())
                .quantity(request.quantity())
                .unit(request.unit())
                .minimumThreshold(request.minimumThreshold())
                .type(request.type())
                .build();
        return toResponse(stockItemRepository.save(item));
    }

    public StockItemResponse update(String id, StockItemRequest request) {
        StockItem item = stockItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Stock item not found: " + id));

        // check name uniqueness only if the name changed
        if (!item.getName().equalsIgnoreCase(request.name())
                && stockItemRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Stock item already exists: " + request.name());
        }

        item.setName(request.name().toLowerCase().trim());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item.setMinimumThreshold(request.minimumThreshold());
        item.setType(request.type());
        return toResponse(stockItemRepository.save(item));
    }

    public void delete(String id) {
        if (!stockItemRepository.existsById(id)) {
            throw new NoSuchElementException("Stock item not found: " + id);
        }
        stockItemRepository.deleteById(id);
    }

    // -----------------------------------------------------------------------
    // Unit conversion helpers
    // -----------------------------------------------------------------------
    private static final Map<String, Double> WEIGHT_TO_GRAMS = Map.of(
            "g",  1.0,
            "kg", 1000.0
    );
    private static final Map<String, Double> VOLUME_TO_ML = Map.of(
            "ml",     1.0,
            "liters", 1000.0
    );
    private static final Set<String> COUNT_UNITS = Set.of("pieces", "portions");

    /**
     * Convert {@code amount} expressed in {@code fromUnit} into {@code toUnit}.
     * Supports weight (g <-> kg), volume (ml <-> liters) and count (pieces <-> portions).
     * Throws {@link IllegalArgumentException} when the units belong to different
     * measurement groups (e.g. grams vs liters).
     */
    double convertUnits(double amount, String fromUnit, String toUnit) {
        String from = fromUnit.toLowerCase();
        String to   = toUnit.toLowerCase();
        if (from.equals(to)) return amount;

        if (WEIGHT_TO_GRAMS.containsKey(from) && WEIGHT_TO_GRAMS.containsKey(to)) {
            return amount * WEIGHT_TO_GRAMS.get(from) / WEIGHT_TO_GRAMS.get(to);
        }
        if (VOLUME_TO_ML.containsKey(from) && VOLUME_TO_ML.containsKey(to)) {
            return amount * VOLUME_TO_ML.get(from) / VOLUME_TO_ML.get(to);
        }
        if (COUNT_UNITS.contains(from) && COUNT_UNITS.contains(to)) {
            return amount; // 1 : 1 within the count group
        }
        throw new IllegalArgumentException(
                String.format("Incompatible units: cannot convert '%s' to '%s'", fromUnit, toUnit));
    }

    /**
     * Deduct a recipe ingredient amount from stock, handling unit conversion.
     * E.g. deduct 100 g from a stock entry stored in kg -> deducts 0.1 kg.
     *
     * @param ingredientName stock item name (case-insensitive match)
     * @param amount         quantity to deduct, expressed in {@code recipeUnit}
     * @param recipeUnit     unit used in the recipe (may differ from stock unit)
     */
    public void deduct(String ingredientName, double amount, String recipeUnit) {
        StockItem item = stockItemRepository.findByNameIgnoreCase(ingredientName)
                .orElseThrow(() -> new NoSuchElementException(
                        "Stock item not found for ingredient: " + ingredientName));
        double converted = convertUnits(amount, recipeUnit, item.getUnit());
        item.setQuantity(Math.round((item.getQuantity() - converted) * 100.0) / 100.0);
        stockItemRepository.save(item);
    }

    private StockItemResponse toResponse(StockItem item) {
        boolean low = item.getMinimumThreshold() != null
                && item.getQuantity() < item.getMinimumThreshold();
        StockType resolvedType = item.getType() != null ? item.getType() : inferType(item.getUnit());
        return new StockItemResponse(
                item.getId(),
                item.getName(),
                Math.round(item.getQuantity() * 100.0) / 100.0,
                item.getUnit(),
                item.getMinimumThreshold(),
                low,
                resolvedType
        );
    }

    private StockType inferType(String unit) {
        return switch (unit.toLowerCase()) {
            case "kg", "g" -> StockType.SOLID;
            case "liters", "ml" -> StockType.LIQUID;
            default -> StockType.PORTION;
        };
    }
}
