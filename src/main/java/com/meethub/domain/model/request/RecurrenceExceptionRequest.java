package com.meethub.domain.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecurrenceExceptionRequest {
    public String getExceptionDate() {
        return exceptionDate;
    }

    public void setExceptionDate(String exceptionDate) {
        this.exceptionDate = exceptionDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @NotBlank(message = "Data wyjątku jest wymagana")
    private String exceptionDate;

    @Size(max = 500, message = "Powód nie może przekraczać 500 znaków")
    private String reason;

    // getters i setters
}
