package com.meethub.domain.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecentMessagesRequest {

    @NotNull(message = "Limit nie może być pusty")
    @Min(value = 1, message = "Limit musi być co najmniej 1")
    @Max(value = 100, message = "Limit nie może przekraczać 100")
    private Integer limit;
}
