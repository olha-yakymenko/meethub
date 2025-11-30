package com.meethub.domain.model.enums;

public enum VotingStatus {
    ACTIVE("Aktywne"),
    CLOSED("Zamknięte"),
    CANCELLED("Anulowane"),
    PENDING("Trwajace");

    private final String displayName;

    VotingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}