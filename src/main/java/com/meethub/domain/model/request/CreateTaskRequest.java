// CreateTaskRequest.java
package com.meethub.domain.model.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "Tytuł zadania jest wymagany")
    @Size(min = 3, max = 255, message = "Tytuł musi mieć od 3 do 255 znaków")
    private String title;

    @NotBlank(message = "Opis zadania jest wymagany")
    @Size(min = 10, max = 2000, message = "Opis musi mieć od 10 do 2000 znaków")
    private String description;

    @NotNull(message = "Data zakończenia jest wymagana")
    @Future(message = "Data zakończenia musi być w przyszłości")
    private LocalDateTime deadline;

    private List<Long> assignedUserIds; // Lista użytkowników do przypisania

    @Builder.Default
    private Boolean allowSelfAssignment = true; // Czy użytkownicy mogą sami się przypisywać

    @Builder.Default
    private Integer maxFilesPerUser = 10; // Maksymalna liczba plików na użytkownika

    @Builder.Default
    private Long maxFileSize = 10 * 1024 * 1024L; // 10MB domyślnie

    private List<String> allowedFileTypes; // Dozwolone typy plików (np. ["pdf", "docx", "jpg"])

    // Metody walidacyjne
    public boolean hasAssignedUsers() {
        return assignedUserIds != null && !assignedUserIds.isEmpty();
    }

    public boolean isFileTypeAllowed(String fileType) {
        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
            return true; // Wszystkie typy dozwolone jeśli lista pusta
        }
        return allowedFileTypes.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(fileType));
    }

    public String getAllowedFileTypesAsString() {
        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
            return "Wszystkie typy plików";
        }
        return String.join(", ", allowedFileTypes);
    }


}