// UpdateLocationRequestTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.LocationType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UpdateLocationRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidUpdateRequest() {
        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .name("Updated Conference Room")
                .type(LocationType.PHYSICAL)
                .address("456 Updated Street, Suite 200")
                .city("Krakow")
                .country("Poland")
                .latitude(new BigDecimal("50.064650"))
                .longitude(new BigDecimal("19.944980"))
                .virtualMeetingUrl("") // Not required for physical
                .accessCode("UPDATED123")
                .drivingInstructions("Use main entrance, parking on level -2")
                .timezone("Europe/Warsaw")
                .build();

        var violations = validator.validate(request);

        assertAll("Valid location update request",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("Updated Conference Room", request.getName(),
                        "Name should match"),
                () -> assertEquals(LocationType.PHYSICAL, request.getType(),
                        "Type should be PHYSICAL"),
                () -> assertEquals("456 Updated Street, Suite 200", request.getAddress(),
                        "Address should match"),
                () -> assertEquals("Krakow", request.getCity(),
                        "City should match"),
                () -> assertEquals("Poland", request.getCountry(),
                        "Country should match"),
                () -> assertEquals(new BigDecimal("50.064650"), request.getLatitude(),
                        "Latitude should match"),
                () -> assertEquals(new BigDecimal("19.944980"), request.getLongitude(),
                        "Longitude should match"),
                () -> assertEquals("UPDATED123", request.getAccessCode(),
                        "Access code should match"),
                () -> assertEquals("Use main entrance, parking on level -2",
                        request.getDrivingInstructions(),
                        "Driving instructions should match"),
                () -> assertEquals("Europe/Warsaw", request.getTimezone(),
                        "Timezone should match"),
                () -> assertTrue(request.isAddressValid(),
                        "Address should be valid for physical location"),
                () -> assertTrue(request.isVirtualUrlValid(),
                        "Virtual URL validation should pass for physical location")
        );
    }

    @Test
    void testValidationConstraints() {
        UpdateLocationRequest shortName = UpdateLocationRequest.builder()
                .name("A") // Too short
                .type(LocationType.PHYSICAL)
                .build();

        UpdateLocationRequest missingVirtualUrl = UpdateLocationRequest.builder()
                .name("Virtual Location")
                .type(LocationType.VIRTUAL)
                .address("") // Not required for virtual
                // Missing virtualMeetingUrl
                .build();

        UpdateLocationRequest invalidCoordinates = UpdateLocationRequest.builder()
                .name("Valid Name")
                .type(LocationType.PHYSICAL)
                .latitude(new BigDecimal("100.000000")) // Invalid
                .longitude(new BigDecimal("200.000000")) // Invalid
                .build();

        var nameViolations = validator.validate(shortName);
        var urlViolations = validator.validate(missingVirtualUrl);
        var coordViolations = validator.validate(invalidCoordinates);

        assertAll("Constraint violations",
                () -> assertEquals(2, nameViolations.size(),
                        "Short name should have 2 violation"),
                () -> assertTrue(nameViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Nazwa musi mieć 2-200 znaków")),
                        "Violation should mention name length"),

                () -> assertFalse(missingVirtualUrl.isVirtualUrlValid(),
                        "Missing virtual URL should be invalid"),
                () -> assertEquals(1, urlViolations.size(),
                        "Missing required field should have 1 violation"),

                () -> assertEquals(3, coordViolations.size(),
                        "Invalid coordinates should have 3 violations"),
                () -> assertTrue(coordViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Szerokość geograficzna musi być między -90 a 90")),
                        "Should have latitude violation"),
                () -> assertTrue(coordViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Długość geograficzna musi być między -180 a 180")),
                        "Should have longitude violation")
        );
    }

    @Test
    void testBusinessValidationMethods() {
        UpdateLocationRequest physicalValid = UpdateLocationRequest.builder()
                .name("Physical")
                .type(LocationType.PHYSICAL)
                .address("Valid Address")
                .virtualMeetingUrl("") // Optional
                .build();

        UpdateLocationRequest physicalInvalid = UpdateLocationRequest.builder()
                .name("Physical")
                .type(LocationType.PHYSICAL)
                .address("") // Invalid for physical
                .build();

        UpdateLocationRequest virtualValid = UpdateLocationRequest.builder()
                .name("Virtual")
                .type(LocationType.VIRTUAL)
                .address("") // Optional for virtual
                .virtualMeetingUrl("https://valid.url")
                .build();

        UpdateLocationRequest virtualInvalid = UpdateLocationRequest.builder()
                .name("Virtual")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("") // Invalid for virtual
                .build();

        assertAll("Business validation methods",
                () -> assertTrue(physicalValid.isAddressValid(),
                        "Valid address for physical location should pass"),
                () -> assertTrue(physicalValid.isVirtualUrlValid(),
                        "Virtual URL not required for physical location"),

                () -> assertFalse(physicalInvalid.isAddressValid(),
                        "Missing address for physical location should fail"),

                () -> assertTrue(virtualValid.isAddressValid(),
                        "Address not required for virtual location"),
                () -> assertTrue(virtualValid.isVirtualUrlValid(),
                        "Valid virtual URL for virtual location should pass"),

                () -> assertFalse(virtualInvalid.isVirtualUrlValid(),
                        "Missing virtual URL for virtual location should fail")
        );
    }
}