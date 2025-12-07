//package com.meethub.domain.model.enums;
//
//public enum ParticipationStatus {
//    INVITED("Invited"),
//    CONFIRMED("Confirmed"),
//    DECLINED("Declined"),
//    TENTATIVE("Tentative"),
//    WAITING_LIST("Waiting List"),
//    CANCELLED("Cancelled"),
//    PENDING("Pending");
//
//
//    private final String displayName;
//
//    ParticipationStatus(String displayName) {
//        this.displayName = displayName;
//    }
//
//    public String getDisplayName() {
//        return displayName;
//    }
//}



package com.meethub.domain.model.enums;

public enum ParticipationStatus {
    // Statusy zaproszeń
    INVITED("Zaproszony"),           // Otrzymał zaproszenie
    PENDING("Oczekujący"),           // Oczekuje na akceptację (dla spotkań prywatnych)
    DECLINED("Odrzucił"),            // Odrzucił zaproszenie
    CANCELLED("Anulował"),           // Sam anulował uczestnictwo

    // Statusy potwierdzenia
    CONFIRMED("Potwierdzony"),       // Potwierdził uczestnictwo
    TENTATIVE("Niepewny"),           // Może przyjść

    // Lista rezerwowa
    WAITING_LIST("Lista rezerwowa"), // Czeka na wolne miejsce

    // Statusy obecności
    ATTENDED("Był obecny"),          // Faktycznie uczestniczył
    NO_SHOW("Nie przyszedł"),        // Potwierdził ale nie przyszedł
    LEFT_EARLY("Wyszedł wcześniej"), // Opuścił przed końcem
    JOINED_LATE("Dołączył później"), // Dołączył po rozpoczęciu

    // Statusy specjalne
    ORGANIZER("Organizator"),        // Twórca spotkania
    CO_ORGANIZER("Współorganizator"), // Współorganizator
    SPEAKER("Prelegent"),            // Prelegent/prowadzący
    GUEST("Gość");// Specjalny gość


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
        return this == CONFIRMED || this == TENTATIVE ||
                this == ATTENDED || this == JOINED_LATE ||
                this == LEFT_EARLY || this == ORGANIZER ||
                this == CO_ORGANIZER || this == SPEAKER;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED || this == ATTENDED ||
                this == JOINED_LATE || this == LEFT_EARLY;
    }

    public boolean isAttended() {
        return this == ATTENDED || this == JOINED_LATE ||
                this == LEFT_EARLY;
    }

    public boolean isInvitation() {
        return this == INVITED || this == PENDING;
    }

    public boolean canJoin() {
        return this == CONFIRMED || this == TENTATIVE ||
                this == ORGANIZER || this == CO_ORGANIZER ||
                this == SPEAKER || this == GUEST;
    }

    public static ParticipationStatus[] getActiveStatuses() {
        return new ParticipationStatus[] {
                CONFIRMED, TENTATIVE, ATTENDED,
                JOINED_LATE, LEFT_EARLY, ORGANIZER,
                CO_ORGANIZER, SPEAKER, GUEST
        };
    }

    public static ParticipationStatus[] getAttendanceStatuses() {
        return new ParticipationStatus[] {
                ATTENDED, JOINED_LATE, LEFT_EARLY, NO_SHOW
        };
    }
}