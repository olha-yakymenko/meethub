// MeetingResourceResponse.java
package com.meethub.domain.model.response;

import com.meethub.domain.model.enums.AccessLevel;
import com.meethub.domain.model.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MeetingResourceResponse {
    private Long id;
    private String filename;
    private String originalFilename;
    private String description;
    private Long fileSize;
    private String fileSizeFormatted;
    private String mimeType;
    private ResourceType resourceType;
    private Set<String> tags;
    private Integer version;
    private Boolean isCurrent;
    private AccessLevel accessLevel;
    private UserResponse uploadedBy;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private String previewUrl;
    private Boolean canEdit;
    private Boolean canDelete;

}