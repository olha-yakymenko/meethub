
package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VotingOptionRequest {

    @NotNull(message = "Data opcji jest wymagana")
    @Future(message = "Option date must be in the future")
    private LocalDateTime optionDate;

    @Min(value = 15, message = "Czas trwania musi wynosić co najmniej 15 minut")
    @Max(value = 480, message = "Czas trwania nie może przekraczać 8 godzin")
    private Integer durationMinutes;
}