package com.meethub.domain.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NearbyMeetingsRequest {

    @NotNull(message = "Szerokość geograficzna nie może być pusta")
    @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
    @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
    private Double latitude;

    @NotNull(message = "Długość geograficzna nie może być pusta")
    @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
    @DecimalMax(value = "180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
    private Double longitude;

    @Min(value = 100, message = "Promień musi być co najmniej 100 metrów")
    @Max(value = 100000, message = "Promień nie może przekraczać 100000 metrów")
    private Double radius = 5000.0; // domyślna wartość
}
