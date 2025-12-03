package com.restaurant.Apollo.Reservations.dto;

import java.util.List;

public class AvailabilityResponse {
    private List<TimeSlotAvailability> availableSlots;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(List<TimeSlotAvailability> availableSlots) {
        this.availableSlots = availableSlots;
    }

    public List<TimeSlotAvailability> getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(List<TimeSlotAvailability> availableSlots) {
        this.availableSlots = availableSlots;
    }

    public static class TimeSlotAvailability {
        private String timeSlot;
        private String startTime;
        private String endTime;
        private List<TableAvailability> tables;

        public TimeSlotAvailability() {
        }

        public TimeSlotAvailability(String timeSlot, String startTime, String endTime, List<TableAvailability> tables) {
            this.timeSlot = timeSlot;
            this.startTime = startTime;
            this.endTime = endTime;
            this.tables = tables;
        }

        public String getTimeSlot() {
            return timeSlot;
        }

        public void setTimeSlot(String timeSlot) {
            this.timeSlot = timeSlot;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public List<TableAvailability> getTables() {
            return tables;
        }

        public void setTables(List<TableAvailability> tables) {
            this.tables = tables;
        }
    }

    public static class TableAvailability {
        private Long id;
        private Integer tableNumber;
        private Integer capacity;
        private Double xPosition;
        private Double yPosition;
        private Double width;
        private Double height;
        private Boolean available;

        public TableAvailability() {
        }

        public TableAvailability(Long id, Integer tableNumber, Integer capacity, 
                                Double xPosition, Double yPosition, Double width, 
                                Double height, Boolean available) {
            this.id = id;
            this.tableNumber = tableNumber;
            this.capacity = capacity;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
            this.width = width;
            this.height = height;
            this.available = available;
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

        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }
    }
}
