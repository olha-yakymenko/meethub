package com.meethub.domain.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SaveTemplateRequest {
    @NotBlank(message = "Nazwa szablonu jest wymagana")
    @Size(min = 3, max = 100, message = "Nazwa szablonu musi mieć od 3 do 100 znaków")
    private String templateName;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}