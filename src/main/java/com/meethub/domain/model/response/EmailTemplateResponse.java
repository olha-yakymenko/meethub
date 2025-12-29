package com.meethub.domain.model.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmailTemplateResponse {
    private Long id;
    private String templateKey;
    private String name;
    private String subject;
    private String bodyTemplate;
    private String language;
    private String category;
    private String description;
    private String variablesHelp;
    private Boolean isActive;
    private Integer version;
    private String channel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String availableVariables;
}