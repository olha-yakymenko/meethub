//// MeetingResourceRequest.java
//package com.meethub.domain.model.request;
//
//import com.meethub.domain.model.enums.AccessLevel;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.Data;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Set;
//
//@Data
//public class MeetingResourceRequest {
//    @NotBlank(message = "Original filename is required")
//    private String originalFilename;
//
//    private String description;
//
//    @NotNull(message = "File is required")
//    private MultipartFile file;
//
//    private Set<String> tags;
//
//    @NotNull(message = "Access level is required")
//    private AccessLevel accessLevel;
//}





package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AccessLevel;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class MeetingResourceRequest {

    @NotBlank(message = "Original filename is required")
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    private String originalFilename;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "File is required")
    private MultipartFile file;

    @Size(max = 10, message = "Cannot have more than 10 tags")
    private Set<String> tags;

    @NotNull(message = "Access level is required")
    private AccessLevel accessLevel;

    // Walidacja pliku
    @AssertTrue(message = "File size exceeds limit")
    public boolean isFileSizeValid() {
        if (file == null) return true;
        return file.getSize() <= 10 * 1024 * 1024; // 10MB
    }
}