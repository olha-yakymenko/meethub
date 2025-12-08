package com.meethub.domain.model.enums;

public enum MeetingStatus {
    PLANNED("Planned"),
    CANCELLED("Cancelled"),
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