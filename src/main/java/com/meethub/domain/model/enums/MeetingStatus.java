package com.meethub.domain.model.enums;

public enum MeetingStatus {
    PLANNED("Planned"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    POSTPONED("Postponed"),
    COMPLETED("Completed"),
    ONGOING("Ongoing");

    private final String displayName;

    MeetingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}