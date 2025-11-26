//// EmailTemplateServiceImpl.java
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.EmailTemplate;
//import com.meethub.domain.model.request.CreateEmailTemplateRequest;
//import com.meethub.domain.model.request.UpdateEmailTemplateRequest;
//import com.meethub.domain.model.response.EmailTemplateResponse;
//import com.meethub.domain.repository.jpa.EmailTemplateRepository;
//import com.meethub.domain.service.EmailTemplateService;
//import com.meethub.exception.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class EmailTemplateServiceImpl implements EmailTemplateService {
//
//    private final EmailTemplateRepository emailTemplateRepository;
//
//    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");
//
//    @Override
//    public EmailTemplate createTemplate(CreateEmailTemplateRequest request) {
//        // Sprawdź czy szablon już istnieje
//        if (emailTemplateRepository.existsByTemplateKeyAndLanguage(request.getTemplateKey(), request.getLanguage())) {
//            throw new IllegalArgumentException("Template with key '" + request.getTemplateKey() + "' and language '" + request.getLanguage() + "' already exists");
//        }
//
//        EmailTemplate template = EmailTemplate.builder()
//                .templateKey(request.getTemplateKey())
//                .name(request.getName())
//                .subject(request.getSubject())
//                .bodyTemplate(request.getBodyTemplate())
//                .language(request.getLanguage())
//                .category(request.getCategory())
//                .description(request.getDescription())
//                .variablesHelp(request.getVariablesHelp())
//                .channel(request.getChannel())
//                .isActive(true)
//                .version(1)
//                .build();
//
//        return emailTemplateRepository.save(template);
//    }
//
//    @Override
//    public EmailTemplate updateTemplate(Long templateId, UpdateEmailTemplateRequest request) {
//        EmailTemplate template = emailTemplateRepository.findById(templateId)
//                .orElseThrow(() -> new ResourceNotFoundException("Email template not found"));
//
//        // Jeśli zmieniono treść, utwórz nową wersję
//        if (request.getBodyTemplate() != null && !request.getBodyTemplate().equals(template.getBodyTemplate())) {
//            return createNewVersion(template.getTemplateKey(), template.getLanguage(), request.getBodyTemplate());
//        }
//
//        // Aktualizuj pozostałe pola
//        if (request.getName() != null) {
//            template.setName(request.getName());
//        }
//        if (request.getSubject() != null) {
//            template.setSubject(request.getSubject());
//        }
//        if (request.getCategory() != null) {
//            template.setCategory(request.getCategory());
//        }
//        if (request.getDescription() != null) {
//            template.setDescription(request.getDescription());
//        }
//        if (request.getVariablesHelp() != null) {
//            template.setVariablesHelp(request.getVariablesHelp());
//        }
//        if (request.getIsActive() != null) {
//            template.setIsActive(request.getIsActive());
//        }
//        if (request.getChannel() != null) {
//            template.setChannel(request.getChannel());
//        }
//
//        template.setUpdatedAt(LocalDateTime.now());
//        return emailTemplateRepository.save(template);
//    }
//
//    @Override
//    public void deleteTemplate(Long templateId) {
//        EmailTemplate template = emailTemplateRepository.findById(templateId)
//                .orElseThrow(() -> new ResourceNotFoundException("Email template not found"));
//        emailTemplateRepository.delete(template);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public EmailTemplateResponse getTemplateById(Long templateId) {
//        EmailTemplate template = emailTemplateRepository.findById(templateId)
//                .orElseThrow(() -> new ResourceNotFoundException("Email template not found"));
//        return mapToResponse(template);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<EmailTemplateResponse> getAllTemplates() {
//        return emailTemplateRepository.findAll().stream()
//                .map(this::mapToResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<EmailTemplateResponse> getTemplatesByCategory(String category) {
//        return emailTemplateRepository.findByCategory(category).stream()
//                .map(this::mapToResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<EmailTemplateResponse> getActiveTemplates() {
//        return emailTemplateRepository.findByIsActiveTrue().stream()
//                .map(this::mapToResponse)
//                .toList();
//    }
//
//    @Override
//    public String personalizeTemplate(String templateKey, String language, Map<String, String> variables) {
//        EmailTemplate template = emailTemplateRepository.findActiveByTemplateKeyAndLanguage(templateKey, language)
//                .orElseThrow(() -> new ResourceNotFoundException("Active template not found for key: " + templateKey + " and language: " + language));
//
//        return personalizeTemplate(template, variables);
//    }
//
//    @Override
//    public String personalizeTemplate(EmailTemplate template, Map<String, String> variables) {
//        String personalized = template.getBodyTemplate();
//
//        for (Map.Entry<String, String> entry : variables.entrySet()) {
//            String placeholder = "{{" + entry.getKey() + "}}";
//            String value = entry.getValue() != null ? entry.getValue() : "";
//            personalized = personalized.replace(placeholder, value);
//        }
//
//        // Zastąp brakujące zmienne pustym stringiem
//        Matcher matcher = VARIABLE_PATTERN.matcher(personalized);
//        while (matcher.find()) {
//            String variable = matcher.group(1);
//            personalized = personalized.replace("{{" + variable + "}}", "");
//        }
//
//        return personalized;
//    }
//
//    @Override
//    public EmailTemplate createNewVersion(String templateKey, String language, String newBody) {
//        EmailTemplate currentTemplate = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, language)
//                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
//
//        // Deaktywuj starą wersję
//        currentTemplate.setIsActive(false);
//        emailTemplateRepository.save(currentTemplate);
//
//        // Utwórz nową wersję
//        EmailTemplate newVersion = EmailTemplate.builder()
//                .templateKey(currentTemplate.getTemplateKey())
//                .name(currentTemplate.getName())
//                .subject(currentTemplate.getSubject())
//                .bodyTemplate(newBody)
//                .language(currentTemplate.getLanguage())
//                .category(currentTemplate.getCategory())
//                .description(currentTemplate.getDescription())
//                .variablesHelp(currentTemplate.getVariablesHelp())
//                .channel(currentTemplate.getChannel())
//                .isActive(true)
//                .version(currentTemplate.getVersion() + 1)
//                .build();
//
//        return emailTemplateRepository.save(newVersion);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<EmailTemplate> getTemplateVersions(String templateKey, String language) {
//        return emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, language).stream()
//                .sorted(Comparator.comparing(EmailTemplate::getVersion).reversed())
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean validateTemplateVariables(String templateKey, Map<String, String> variables) {
//        try {
//            EmailTemplate template = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, "pl")
//                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
//
//            Set<String> requiredVariables = extractVariables(template.getBodyTemplate());
//            requiredVariables.addAll(extractVariables(template.getSubject()));
//
//            for (String requiredVar : requiredVariables) {
//                if (!variables.containsKey(requiredVar) || variables.get(requiredVar) == null) {
//                    return false;
//                }
//            }
//
//            return true;
//        } catch (Exception e) {
//            log.error("Error validating template variables: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<String> getRequiredVariables(String templateKey) {
//        try {
//            EmailTemplate template = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, "pl")
//                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
//
//            Set<String> variables = extractVariables(template.getBodyTemplate());
//            variables.addAll(extractVariables(template.getSubject()));
//
//            return new ArrayList<>(variables);
//        } catch (Exception e) {
//            log.error("Error getting required variables: {}", e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    // Metody pomocnicze
//    private Set<String> extractVariables(String text) {
//        Set<String> variables = new HashSet<>();
//        if (text == null) return variables;
//
//        Matcher matcher = VARIABLE_PATTERN.matcher(text);
//        while (matcher.find()) {
//            variables.add(matcher.group(1).trim());
//        }
//
//        return variables;
//    }
//
//    private EmailTemplateResponse mapToResponse(EmailTemplate template) {
//        EmailTemplateResponse response = new EmailTemplateResponse();
//        response.setId(template.getId());
//        response.setTemplateKey(template.getTemplateKey());
//        response.setName(template.getName());
//        response.setSubject(template.getSubject());
//        response.setBodyTemplate(template.getBodyTemplate());
//        response.setLanguage(template.getLanguage());
//        response.setCategory(template.getCategory());
//        response.setDescription(template.getDescription());
//        response.setVariablesHelp(template.getVariablesHelp());
//        response.setIsActive(template.getIsActive());
//        response.setVersion(template.getVersion());
//        response.setChannel(template.getChannel());
//        response.setCreatedAt(template.getCreatedAt());
//        response.setUpdatedAt(template.getUpdatedAt());
//        response.setAvailableVariables(template.getAvailableVariables());
//        return response;
//    }
//}