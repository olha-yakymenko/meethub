package com.meethub.domain.model.enums;

public enum AccessLevel {
    PUBLIC("Public"),
    PARTICIPANTS("Participants"),
    ORGANIZERS("Organizers"),
    PRIVATE("Private");

    private final String displayName;

    AccessLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}