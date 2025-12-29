
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AccessLevel;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateMeetingResourceRequest {

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 10, message = "Cannot have more than 10 tags")
    private Set<String> tags;

    @NotNull(message = "Access level is required")
    private AccessLevel accessLevel;
}