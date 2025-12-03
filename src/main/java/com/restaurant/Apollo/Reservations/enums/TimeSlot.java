package com.restaurant.Apollo.Reservations.enums;

import java.time.LocalTime;

public enum TimeSlot {
    SLOT_10_12(LocalTime.of(10, 0), LocalTime.of(12, 0)),
    SLOT_12_14(LocalTime.of(12, 0), LocalTime.of(14, 0)),
    SLOT_14_16(LocalTime.of(14, 0), LocalTime.of(16, 0)),
    SLOT_16_18(LocalTime.of(16, 0), LocalTime.of(18, 0)),
    SLOT_18_20(LocalTime.of(18, 0), LocalTime.of(20, 0)),
    SLOT_20_22(LocalTime.of(20, 0), LocalTime.of(22, 0));

    private final LocalTime startTime;
    private final LocalTime endTime;

    TimeSlot(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getDisplayString() {
        return startTime.toString() + " - " + endTime.toString();
    }

    public static TimeSlot fromStartTime(LocalTime time) {
        for (TimeSlot slot : values()) {
            if (slot.startTime.equals(time)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("Invalid time slot: " + time);
    }
}
