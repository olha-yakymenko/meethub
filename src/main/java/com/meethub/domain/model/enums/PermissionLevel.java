package com.meethub.domain.model.enums;
//
//public enum PermissionLevel {
//    VIEWER("Viewer"),
//    PARTICIPANT("Participant"),
//    CONTRIBUTOR("Contributor"),
//    ORGANIZER("Organizer"),
//    MODERATOR("Moderator");
//
//    private final String displayName;
//
//    PermissionLevel(String displayName) {
//        this.displayName = displayName;
//    }
//
//    public String getDisplayName() {
//        return displayName;
//    }
//}




public enum PermissionLevel {
    VIEWER("Widz", false, false, false),        // Tylko obserwuje
    PARTICIPANT("Uczestnik", true, false, false), // Może uczestniczyć
    CONTRIBUTOR("Współtwórca", true, true, false), // Może dodawać zasoby
    MODERATOR("Moderator", true, true, true),   // Może zarządzać uczestnikami
    ORGANIZER("Organizator", true, true, true); // Pełne uprawnienia

    private final String displayName;
    private final boolean canParticipate;
    private final boolean canContribute;
    private final boolean canManage;

    PermissionLevel(String displayName, boolean canParticipate,
                    boolean canContribute, boolean canManage) {
        this.displayName = displayName;
        this.canParticipate = canParticipate;
        this.canContribute = canContribute;
        this.canManage = canManage;
    }

    public String getDisplayName() { return displayName; }
    public boolean canParticipate() { return canParticipate; }
    public boolean canContribute() { return canContribute; }
    public boolean canManage() { return canManage; }
}