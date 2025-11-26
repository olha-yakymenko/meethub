// UpdateMeetingResourceRequest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AccessLevel;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateMeetingResourceRequest {
    private String description;
    private Set<String> tags;
    private AccessLevel accessLevel;
}