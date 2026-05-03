package com.restaurant.reservations.dto;

public class CancelReservationRequest {
    private String cancelReason;

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
