package com.meethub.domain.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class GenerateOccurrencesRequest {
    @Min(value = 1, message = "Liczba wystąpień musi być co najmniej 1")
    @Max(value = 50, message = "Liczba wystąpień nie może przekraczać 50")
    private int count = 5; // domyślna wartość

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
