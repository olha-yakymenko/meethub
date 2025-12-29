package com.meethub.domain.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.query.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingsListRequest {

    @Min(value = 0, message = "Numer strony musi być nieujemny")
    private Integer page = 0;

    @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
    @Max(value = 50, message = "Rozmiar strony nie może przekraczać 50")
    private Integer size = 12;

    @Size(max = 100, message = "Wyszukiwanie nie może przekraczać 100 znaków")
    private String search;

    private String type;

    private String status;

}