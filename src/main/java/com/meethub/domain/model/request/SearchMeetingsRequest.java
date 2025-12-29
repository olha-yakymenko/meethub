package com.meethub.domain.model.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchMeetingsRequest {

    @Size(max = 100, message = "Zapytanie nie może przekraczać 100 znaków")
    private String query;

    private String type;

    private String status;
}