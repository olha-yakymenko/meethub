package com.meethub.domain.model.enums;

public enum ParticipationStatus {
    INVITED("Invited"),
    CONFIRMED("Confirmed"),
    DECLINED("Declined"),
    TENTATIVE("Tentative"),
    WAITING_LIST("Waiting List"),
    CANCELLED("Cancelled"),
    PENDING("Pending");


    private final String displayName;

    ParticipationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}