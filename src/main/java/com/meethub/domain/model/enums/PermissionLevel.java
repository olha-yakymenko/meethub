package com.meethub.domain.model.enums;

public enum PermissionLevel {
    VIEWER("Viewer"),
    PARTICIPANT("Participant"),
    CONTRIBUTOR("Contributor"),
    MODERATOR("Moderator");

    private final String displayName;

    PermissionLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}