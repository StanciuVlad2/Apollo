package com.restaurant.Apollo.Stock.repository;

import com.restaurant.Apollo.Stock.model.StockItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends ElasticsearchRepository<StockItem, String> {
    Optional<StockItem> findByNameIgnoreCase(String name);
    List<StockItem> findByNameContainingIgnoreCase(String name);
    List<StockItem> findByQuantityLessThan(Double threshold);
    boolean existsByNameIgnoreCase(String name);
}
