package com.meethub.domain.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VoteRequest {
    @NotNull(message = "Musisz wybrać opcje")
    private List<Long> optionIds;

    private List<Integer> preferenceOrder; // Dla głosowania rankingowego
}
