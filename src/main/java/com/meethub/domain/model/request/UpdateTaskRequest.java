//// UpdateTaskRequest.java
//package com.meethub.domain.model.request;
//
//import jakarta.validation.constraints.Future;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class UpdateTaskRequest {
//    @NotBlank(message = "Tytuł zadania jest wymagany")
//    @Size(min = 3, max = 255, message = "Tytuł musi mieć od 3 do 255 znaków")
//    private String title;
//
//    @NotBlank(message = "Opis zadania jest wymagany")
//    @Size(min = 10, max = 2000, message = "Opis musi mieć od 10 do 2000 znaków")
//    private String description;
//
//    @NotNull(message = "Data zakończenia jest wymagana")
//    @Future(message = "Data zakończenia musi być w przyszłości")
//    private LocalDateTime deadline;
//
//    @Builder.Default
//    private Boolean allowSelfAssignment = true;
//
////    @Builder.Default
////    private Integer maxFilesPerUser = 10;
////
////    @Builder.Default
////    private Long maxFileSize = 10 * 1024 * 1024L;
//
//    // DODAJ TO POLE jeśli chcesz edytować też typy plików
//    private List<String> allowedFileTypes;
//
//    // DODAJ TE METODY
//    public String getAllowedFileTypesAsString() {
//        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
//            return null;
//        }
//        return String.join(",", allowedFileTypes);
//    }
//
//    public boolean isFileTypeAllowed(String fileExtension) {
//        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
//            return true;
//        }
//        String cleanExtension = fileExtension.replace(".", "").toLowerCase();
//        return allowedFileTypes.stream()
//                .anyMatch(allowed -> allowed.replace(".", "").equalsIgnoreCase(cleanExtension));
//    }
//}




package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
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
public class UpdateTaskRequest {

    @NotBlank(message = "Tytuł zadania jest wymagany")
    @Size(min = 3, max = 255, message = "Tytuł musi mieć od 3 do 255 znaków")
    private String title;

    @NotBlank(message = "Opis zadania jest wymagany")
    @Size(min = 10, max = 2000, message = "Opis musi mieć od 10 do 2000 znaków")
    private String description;

    @NotNull(message = "Data zakończenia jest wymagana")
    @Future(message = "Data zakończenia musi być w przyszłości")
    private LocalDateTime deadline;

    @NotNull(message = "Allow self-assignment must be specified")
    @Builder.Default
    private Boolean allowSelfAssignment = true;

    @Size(max = 20, message = "Cannot have more than 20 file types")
    private List<String> allowedFileTypes;

    // Walidacja formatów plików
    @AssertTrue(message = "Invalid file type format. Use extensions like: pdf, docx, jpg")
    public boolean isValidFileTypes() {
        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
            return true;
        }
        return allowedFileTypes.stream()
                .allMatch(type -> type != null &&
                        type.matches("^[a-zA-Z0-9]{1,10}$"));
    }

    public String getAllowedFileTypesAsString() {
        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
            return null;
        }
        return String.join(",", allowedFileTypes);
    }

    public boolean isFileTypeAllowed(String fileExtension) {
        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
            return true;
        }
        String cleanExtension = fileExtension.replace(".", "").toLowerCase();
        return allowedFileTypes.stream()
                .anyMatch(allowed -> allowed.replace(".", "").equalsIgnoreCase(cleanExtension));
    }
}