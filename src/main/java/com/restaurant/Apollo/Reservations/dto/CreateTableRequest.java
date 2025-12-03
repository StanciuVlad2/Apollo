package com.restaurant.Apollo.Reservations.dto;

public class CreateTableRequest {
    private Integer tableNumber;
    private Integer capacity;
    private Double xPosition;
    private Double yPosition;
    private Double width;
    private Double height;

    // Constructors
    public CreateTableRequest() {
    }

    public CreateTableRequest(Integer tableNumber, Integer capacity, Double xPosition, Double yPosition) {
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = 100.0;
        this.height = 100.0;
    }

    // Getters and Setters
    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Double getXPosition() {
        return xPosition;
    }

    public void setXPosition(Double xPosition) {
        this.xPosition = xPosition;
    }

    public Double getYPosition() {
        return yPosition;
    }

    public void setYPosition(Double yPosition) {
        this.yPosition = yPosition;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }
}
