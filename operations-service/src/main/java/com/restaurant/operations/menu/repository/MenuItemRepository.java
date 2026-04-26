package com.restaurant.operations.menu.repository;

import com.restaurant.operations.menu.model.MenuItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface MenuItemRepository extends ElasticsearchRepository<MenuItem, String> {
    List<MenuItem> findByNameContainingIgnoreCase(String name);
    List<MenuItem> findByCategory(String category);
    List<MenuItem> findByAvailableTrue();
    boolean existsByNameIgnoreCase(String name);
}
