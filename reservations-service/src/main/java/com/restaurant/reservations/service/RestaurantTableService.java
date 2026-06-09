package com.restaurant.reservations.service;

import com.restaurant.reservations.dto.*;
import com.restaurant.reservations.enums.ReservationStatus;
import com.restaurant.reservations.model.RestaurantTable;
import com.restaurant.reservations.repository.ReservationRepository;
import com.restaurant.reservations.repository.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantTableService {

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Transactional
    public TableResponse createTable(CreateTableRequest request) {
        if (tableRepository.existsByTableNumber(request.getTableNumber())) {
            throw new IllegalArgumentException("Table number already exists: " + request.getTableNumber());
        }

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());
        table.setXPosition(request.getXPosition() != null ? request.getXPosition() : 0.0);
        table.setYPosition(request.getYPosition() != null ? request.getYPosition() : 0.0);
        table.setWidth(request.getWidth() != null ? request.getWidth() : 100.0);
        table.setHeight(request.getHeight() != null ? request.getHeight() : 100.0);
        table.setIsActive(true);

        RestaurantTable savedTable = tableRepository.save(table);
        return mapToResponse(savedTable);
    }

    @Transactional
    public TableResponse updateTable(Long id, UpdateTableRequest request) {
        RestaurantTable table = tableRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));

        if (request.getTableNumber() != null && !request.getTableNumber().equals(table.getTableNumber())) {
            if (tableRepository.existsByTableNumber(request.getTableNumber())) {
                throw new IllegalArgumentException("Table number already exists: " + request.getTableNumber());
            }
            table.setTableNumber(request.getTableNumber());
        }

        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }
        if (request.getXPosition() != null) {
            table.setXPosition(request.getXPosition());
        }
        if (request.getYPosition() != null) {
            table.setYPosition(request.getYPosition());
        }
        if (request.getWidth() != null) {
            table.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            table.setHeight(request.getHeight());
        }
        if (request.getActive() != null) {
            table.setIsActive(request.getActive());
        }

        RestaurantTable updatedTable = tableRepository.save(table);
        return mapToResponse(updatedTable);
    }

    @Transactional
    public void deleteTable(Long id) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));
        if (reservationRepository.existsByTableIdAndStatus(id, ReservationStatus.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete table with active reservations");
        }
        if (reservationRepository.existsByTableId(id)) {
            // Historical reservations (cancelled/completed) hold a FK reference — deactivate instead
            table.setIsActive(false);
            tableRepository.save(table);
            return;
        }
        tableRepository.deleteById(id);
    }

    public TableResponse getTable(Long id) {
        RestaurantTable table = tableRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));
        return mapToResponse(table);
    }

    public List<TableResponse> getAllTables() {
        return tableRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<TableResponse> getActiveTables() {
        return tableRepository.findAllByIsActiveTrue().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<TableResponse> getTablesByCapacity(Integer capacity) {
        return tableRepository.findAllByCapacityAndIsActiveTrue(capacity).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private TableResponse mapToResponse(RestaurantTable table) {
        return new TableResponse(
            table.getId(),
            table.getTableNumber(),
            table.getCapacity(),
            table.getXPosition(),
            table.getYPosition(),
            table.getWidth(),
            table.getHeight(),
            table.getIsActive()
        );
    }
}
