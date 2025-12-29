package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeStatusRequest {

    @NotNull(message = "Status jest wymagany")
    private MeetingStatus status;

    @Size(max = 500, message = "Powód zmiany statusu nie może przekraczać 500 znaków")
    private String reason;
}