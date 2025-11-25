package com.meethub.domain.model.enums;

public enum MeetingType {
    ONLINE("Online"),
    IN_PERSON("Osobiście"),
    HYBRID("Hybrydowe");

    private final String displayName;

    MeetingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}