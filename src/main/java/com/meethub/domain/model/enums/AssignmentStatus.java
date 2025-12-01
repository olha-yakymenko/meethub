// AssignmentStatus.java
package com.meethub.domain.model.enums;

public enum AssignmentStatus {
    PENDING("Oczekujące"),
    ASSIGNED("Przypisane"),
    IN_PROGRESS("W trakcie"),
    COMPLETED("Zakończone"),
    REJECTED("Odrzucone"),
    CANCELLED("Anulowane");

    private final String displayName;

    AssignmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ASSIGNED || this == IN_PROGRESS;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean canBeUpdatedTo(AssignmentStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == ASSIGNED || newStatus == REJECTED;
            case ASSIGNED:
                return newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS:
                return newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED:
                return newStatus == IN_PROGRESS; // Możliwość cofnięcia
            case REJECTED:
                return newStatus == ASSIGNED;
            case CANCELLED:
                return newStatus == ASSIGNED;
            default:
                return false;
        }
    }

    public static AssignmentStatus fromString(String status) {
        if (status == null) return null;
        try {
            return AssignmentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}