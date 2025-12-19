package com.meethub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MeethubEmailValidator implements ConstraintValidator<MeethubEmail, String> {

    private static final String REQUIRED_DOMAIN = ".com";

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return true;
        }

        String normalizedEmail = email.toLowerCase().trim();
        return normalizedEmail.endsWith(REQUIRED_DOMAIN.toLowerCase());
    }
}