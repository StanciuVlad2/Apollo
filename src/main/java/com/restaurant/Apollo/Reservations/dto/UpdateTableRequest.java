package com.restaurant.Apollo.Reservations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateTableRequest {
    @JsonProperty("tableNumber")
    private Integer tableNumber;
    
    @JsonProperty("capacity")
    private Integer capacity;
    
    @JsonProperty("xPosition")
    private Double xPosition;
    
    @JsonProperty("yPosition")
    private Double yPosition;
    
    @JsonProperty("width")
    private Double width;
    
    @JsonProperty("height")
    private Double height;
    
    @JsonProperty("active")
    private Boolean isActive;

    // Constructor
    public UpdateTableRequest() {
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

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
