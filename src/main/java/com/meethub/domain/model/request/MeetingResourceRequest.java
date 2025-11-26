// MeetingResourceRequest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class MeetingResourceRequest {
    @NotBlank(message = "Original filename is required")
    private String originalFilename;

    private String description;

    @NotNull(message = "File is required")
    private MultipartFile file;

    private Set<String> tags;

    @NotNull(message = "Access level is required")
    private AccessLevel accessLevel;
}