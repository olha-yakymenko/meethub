// UpdateParticipantRequestTest.java
package request;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateParticipantRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidUpdateRequest() {
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        request.setStatus(ParticipationStatus.PENDING);
        request.setPermissionLevel(PermissionLevel.MODERATOR);
        request.setComment("Promoted to moderator role");
        request.setSendNotification(true);

        var violations = validator.validate(request);

        assertAll("Valid participant update request",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals(ParticipationStatus.PENDING, request.getStatus(),
                        "Status should be ACCEPTED"),
                () -> assertEquals(PermissionLevel.MODERATOR, request.getPermissionLevel(),
                        "Permission level should be MODERATOR"),
                () -> assertEquals("Promoted to moderator role", request.getComment(),
                        "Comment should match"),
                () -> assertTrue(request.getSendNotification(),
                        "Send notification should be true")
        );
    }

    @Test
    void testValidationConstraints() {
        UpdateParticipantRequest nullStatus = new UpdateParticipantRequest();
        nullStatus.setPermissionLevel(PermissionLevel.PARTICIPANT);
        // status is null

        UpdateParticipantRequest nullPermission = new UpdateParticipantRequest();
        nullPermission.setStatus(ParticipationStatus.PENDING);
        // permissionLevel is null

        UpdateParticipantRequest bothNull = new UpdateParticipantRequest();
        // Both status and permissionLevel are null

        var statusViolations = validator.validate(nullStatus);
        var permissionViolations = validator.validate(nullPermission);
        var bothViolations = validator.validate(bothNull);

        assertAll("Constraint violations",
                () -> assertEquals(1, statusViolations.size(),
                        "Null status should have 1 violation"),
                () -> assertTrue(statusViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Status is required")),
                        "Violation should mention status requirement"),

                () -> assertEquals(1, permissionViolations.size(),
                        "Null permission level should have 1 violation"),
                () -> assertTrue(permissionViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Permission level is required")),
                        "Violation should mention permission level requirement"),

                () -> assertEquals(2, bothViolations.size(),
                        "Both null should have 2 violations"),
                () -> assertTrue(bothViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Status is required")),
                        "Should have status violation"),
                () -> assertTrue(bothViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Permission level is required")),
                        "Should have permission level violation")
        );
    }

    @Test
    void testDefaultValue() {
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        request.setStatus(ParticipationStatus.DECLINED);
        request.setPermissionLevel(PermissionLevel.VIEWER);

        assertAll("Default value test",
                () -> assertTrue(request.getSendNotification(),
                        "Default sendNotification should be true"),
                () -> assertNull(request.getComment(),
                        "Default comment should be null")
        );
    }
}