package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.VotingType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateVotingRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldCreateValidVotingRequest() {
        // Given
        var option1 = new VotingOptionRequest();
        option1.setOptionDate(LocalDateTime.now().plusDays(1));
        option1.setDurationMinutes(60);

        var option2 = new VotingOptionRequest();
        option2.setOptionDate(LocalDateTime.now().plusDays(2));
        option2.setDurationMinutes(90);

        var request = new CreateVotingRequest();
        request.setTitle("Team Meeting Vote");
        request.setDescription("Choose best time for team meeting");
        request.setType(VotingType.MULTIPLE_CHOICE);
        request.setMaxChoices(2);
        request.setAllowSuggestions(true);
        request.setDeadlineDate(LocalDateTime.now().plusDays(7));
        request.setAutoClose(true);
        request.setOptions(List.of(option1, option2));

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertEquals("Team Meeting Vote", request.getTitle()),
                () -> assertEquals(VotingType.MULTIPLE_CHOICE, request.getType()),
                () -> assertEquals(2, request.getMaxChoices()),
                () -> assertTrue(request.getAllowSuggestions()),
                () -> assertTrue(request.getAutoClose()),
                () -> assertEquals(2, request.getOptions().size())
        );
    }
}