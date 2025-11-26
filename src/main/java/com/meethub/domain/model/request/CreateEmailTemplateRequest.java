// CreateEmailTemplateRequest.java
package com.meethub.domain.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateEmailTemplateRequest {

    @NotBlank(message = "Template key is required")
    private String templateKey;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    @NotBlank(message = "Language is required")
    private String language = "pl";

    private String category;
    private String description;
    private String variablesHelp;
    private String channel = "EMAIL";
}
