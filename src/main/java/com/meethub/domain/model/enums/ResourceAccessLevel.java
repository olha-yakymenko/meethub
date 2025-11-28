//// src/main/java/com/meethub/domain/model/enums/ResourceAccessLevel.java
//package com.meethub.domain.model.enums;
//
//public enum ResourceAccessLevel {
//    PUBLIC,         // Dostęp publiczny dla wszystkich
//    PARTICIPANTS,   // Tylko dla uczestników spotkania
//    ORGANIZERS      // Tylko dla organizatorów
//}



// src/main/java/com/meethub/domain/model/enums/ResourceAccessLevel.java
package com.meethub.domain.model.enums;

public enum ResourceAccessLevel {
    NONE,       // Brak dostępu
    VIEW,       // Tylko podgląd
    DOWNLOAD,   // Podgląd i pobieranie
    UPLOAD,     // Podgląd, pobieranie, dodawanie
    MANAGE      // Pełne zarządzanie (organizator)
}