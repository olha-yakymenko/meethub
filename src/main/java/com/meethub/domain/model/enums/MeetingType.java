package com.meethub.domain.model.enums;

public enum MeetingType {
    ONLINE("Online"),
    IN_PERSON("Osobiście"),
    PHYSICAL("Physical"),
    HYBRID("Hybrydowe");

    private final String displayName;

    MeetingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MeetingType fromDisplayName(String name) {
        if (name == null) return null;
        for (MeetingType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

}