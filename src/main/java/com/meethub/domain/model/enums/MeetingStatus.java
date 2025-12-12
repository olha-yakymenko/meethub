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

    public static MeetingStatus fromDisplayName(String name) {
        if (name == null) return null;
        for (MeetingStatus s : values()) {
            if (s.getDisplayName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

}