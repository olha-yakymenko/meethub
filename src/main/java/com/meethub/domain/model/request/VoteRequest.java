

package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class VoteRequest {

    @NotNull(message = "Musisz wybrać opcje")
    @NotEmpty(message = "At least one option must be selected")
    private List<Long> optionIds;

    private List<Integer> preferenceOrder;

    @AssertTrue(message = "Preference order must match selected options")
    public boolean isValidPreferenceOrder() {
        if (preferenceOrder == null || preferenceOrder.isEmpty()) {
            return true;
        }
        return optionIds.size() == preferenceOrder.size();
    }
}