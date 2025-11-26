
// UpdateEmailTemplateRequest.java
package com.meethub.domain.model.request;

import lombok.Data;

@Data
public class UpdateEmailTemplateRequest {
    private String name;
    private String subject;
    private String bodyTemplate;
    private String category;
    private String description;
    private String variablesHelp;
    private Boolean isActive;
    private String channel;
}
