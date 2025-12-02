package com.meethub.domain.model.enums;//package com.meethub.domain.model.enums;
//
//public enum UserRole {
//    PARTICIPANT, ORGANIZER, MODERATOR, ADMIN
//}



public enum UserRole {
    PARTICIPANT,           // Zwykły użytkownik
    ORGANIZER,      // Może tworzyć spotkania
    MODERATOR,      // Moderator systemu
    ADMIN;          // Administrator

    // Mapowanie na PermissionLevel domyślnie
    public PermissionLevel getDefaultPermissionLevel() {
        return switch (this) {
            case ADMIN -> PermissionLevel.ORGANIZER;
            case ORGANIZER -> PermissionLevel.ORGANIZER;
            case MODERATOR -> PermissionLevel.MODERATOR;
            case PARTICIPANT -> PermissionLevel.PARTICIPANT;
        };
    }
}