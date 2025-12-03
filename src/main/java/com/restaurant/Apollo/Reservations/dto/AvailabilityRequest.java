package com.restaurant.Apollo.Reservations.dto;

import java.time.LocalDate;
import java.util.List;

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
