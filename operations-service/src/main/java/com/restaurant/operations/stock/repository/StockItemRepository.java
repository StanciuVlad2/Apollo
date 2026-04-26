package com.restaurant.operations.stock.repository;

import com.restaurant.operations.stock.model.StockItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends ElasticsearchRepository<StockItem, String> {
    Optional<StockItem> findByNameIgnoreCase(String name);
    List<StockItem> findByNameContainingIgnoreCase(String name);
    Page<StockItem> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<StockItem> findByQuantityLessThan(Double threshold);
    boolean existsByNameIgnoreCase(String name);
}
