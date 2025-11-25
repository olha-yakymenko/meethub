package com.meethub.domain.model.enums;

public enum ResourceType {
    DOCUMENT("Document"),
    PRESENTATION("Presentation"),
    IMAGE("Image"),
    VIDEO("Video"),
    AUDIO("Audio"),
    OTHER("Other");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}