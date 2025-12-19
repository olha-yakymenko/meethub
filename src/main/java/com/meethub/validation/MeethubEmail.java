package com.meethub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MeethubEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MeethubEmail {
    String message() default "Email musi być w domenie .com";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}