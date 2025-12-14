//// CreateEmailTemplateRequest.java
//package com.meethub.domain.model.request;
//
//import jakarta.validation.constraints.NotBlank;
//import lombok.Data;
//
//@Data
//public class CreateEmailTemplateRequest {
//
//    @NotBlank(message = "Template key is required")
//    private String templateKey;
//
//    @NotBlank(message = "Name is required")
//    private String name;
//
//    @NotBlank(message = "Subject is required")
//    private String subject;
//
//    @NotBlank(message = "Body template is required")
//    private String bodyTemplate;
//
//    @NotBlank(message = "Language is required")
//    private String language = "pl";
//
//    private String category;
//    private String description;
//    private String variablesHelp;
//    private String channel = "EMAIL";
//}





package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateEmailTemplateRequest {

    @NotBlank(message = "Template key is required")
    @Size(max = 100, message = "Template key cannot exceed 100 characters")
    private String templateKey;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String name;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    private String subject;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    @NotBlank(message = "Language is required")
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$",
            message = "Language must be like: pl or en-US")
    private String language = "pl";

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private String variablesHelp;

    @Pattern(regexp = "EMAIL|SMS|PUSH", message = "Channel must be EMAIL, SMS or PUSH")
    private String channel = "EMAIL";
}