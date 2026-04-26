package com.restaurant.reservations.dto;

import java.time.LocalDate;

public class AvailabilityRequest {
    private LocalDate date;
    private Integer partySize;

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }
}
