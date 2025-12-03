package com.restaurant.Apollo.Reservations.dto;

public class TableResponse {
    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private Double xPosition;
    private Double yPosition;
    private Double width;
    private Double height;
    private Boolean isActive;

    // Constructors
    public TableResponse() {
    }

    public TableResponse(Long id, Integer tableNumber, Integer capacity, Double xPosition, 
                        Double yPosition, Double width, Double height, Boolean isActive) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
