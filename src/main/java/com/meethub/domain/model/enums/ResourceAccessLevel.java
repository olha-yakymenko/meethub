
package com.meethub.domain.model.enums;

public enum ResourceAccessLevel {
    NONE,       // Brak dostępu
    VIEW,       // Tylko podgląd
    DOWNLOAD,   // Podgląd i pobieranie
    UPLOAD,     // Podgląd, pobieranie, dodawanie
    MANAGE      // Pełne zarządzanie (organizator)
}