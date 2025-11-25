package com.meethub.domain.model.enums;

public enum MeetingVisibility {
    PUBLIC("Publiczne"),
    PRIVATE("Prywatne"),
    INVITE_ONLY("Tylko zaproszeni");

    private final String displayName;

    MeetingVisibility(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}