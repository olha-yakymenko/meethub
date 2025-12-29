package com.meethub.domain.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class NearbyLocationsRequest {

    @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
    @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
    private BigDecimal lat = new BigDecimal("52.2297");

    @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
    @DecimalMax(value = "180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
    private BigDecimal lng = new BigDecimal("21.0122");

    @DecimalMin(value = "0.1", message = "Promień musi być co najmniej 0.1 km")
    @DecimalMax(value = "100.0", message = "Promień nie może przekraczać 100 km")
    private Double radius = 5.0;
}