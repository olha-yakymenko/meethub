
package com.meethub.domain.model.enums;

public enum ParticipationStatus {
    // Statusy zaproszeń
    INVITED("Zaproszony"),           // Otrzymał zaproszenie
    PENDING("Oczekujący"),           // Oczekuje na akceptację (dla spotkań prywatnych)
    DECLINED("Odrzucił"),            // Odrzucił zaproszenie
    CANCELLED("Anulował"),           // Sam anulował uczestnictwo

    CONFIRMED("Potwierdzony"),       // Potwierdził uczestnictwo

    ATTENDED("Był obecny");
//    WAITING_LIST("Lista oczekujących");


    //    PESENT("Obecny");
    private final String displayName;

    ParticipationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Metody pomocnicze
    public boolean isActive() {
        return this == CONFIRMED ||
                this == ATTENDED;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED || this == ATTENDED;
    }

    public boolean isAttended() {
        return this == ATTENDED;
    }

    public boolean isInvitation() {
        return this == INVITED || this == PENDING;
    }

    public boolean canJoin() {
        return this == CONFIRMED;
    }

    public static ParticipationStatus[] getActiveStatuses() {
        return new ParticipationStatus[] {
                CONFIRMED
        };
    }

    public static ParticipationStatus[] getAttendanceStatuses() {
        return new ParticipationStatus[] {
                ATTENDED
        };
    }
}