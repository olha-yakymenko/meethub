package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.LocationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LocationFilterDTO {
    @Size(max = 100, message = "Zapytanie wyszukiwania nie może przekraczać 100 znaków")
    private String query;

    private LocationType type;

    @Size(max = 50, message = "Nazwa miasta nie może przekraczać 50 znaków")
    private String city;

    @Min(value = 0, message = "Numer strony nie może być ujemny")
    private int page = 0;

    @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
    @Max(value = 100, message = "Rozmiar strony nie może przekraczać 100")
    private int size = 20;
}
