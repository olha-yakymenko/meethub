// UpdateUserRequestTest.java
package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateUserRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidUpdateRequest() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+48 123 456 789")
                .build();

        var violations = validator.validate(request);

        assertAll("Valid user update request",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("John", request.getFirstName(),
                        "First name should match"),
                () -> assertEquals("Doe", request.getLastName(),
                        "Last name should match"),
                () -> assertEquals("+48 123 456 789", request.getPhoneNumber(),
                        "Phone number should match")
        );
    }

}