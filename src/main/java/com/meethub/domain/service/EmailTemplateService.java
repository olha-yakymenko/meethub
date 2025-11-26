// EmailTemplateService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.EmailTemplate;
import com.meethub.domain.model.request.CreateEmailTemplateRequest;
import com.meethub.domain.model.request.UpdateEmailTemplateRequest;
import com.meethub.domain.model.response.EmailTemplateResponse;

import java.util.List;
import java.util.Map;

public interface EmailTemplateService {

    EmailTemplate createTemplate(CreateEmailTemplateRequest request);
    EmailTemplate updateTemplate(Long templateId, UpdateEmailTemplateRequest request);
    void deleteTemplate(Long templateId);
    EmailTemplateResponse getTemplateById(Long templateId);
    List<EmailTemplateResponse> getAllTemplates();
    List<EmailTemplateResponse> getTemplatesByCategory(String category);
    List<EmailTemplateResponse> getActiveTemplates();

    // Personalizacja szablonów
    String personalizeTemplate(String templateKey, String language, Map<String, String> variables);
    String personalizeTemplate(EmailTemplate template, Map<String, String> variables);

    // Zarządzanie wersjami
    EmailTemplate createNewVersion(String templateKey, String language, String newBody);
    List<EmailTemplate> getTemplateVersions(String templateKey, String language);

    // Walidacja
    boolean validateTemplateVariables(String templateKey, Map<String, String> variables);
    List<String> getRequiredVariables(String templateKey);
}