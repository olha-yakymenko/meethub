
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Location;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

@Validated
public interface GeocodingService {

    Location geocodeAddress(
            @NotBlank(message = "Adres nie może być pusty")
            @Size(max = 500, message = "Adres nie może przekraczać 500 znaków")
            String address
    );

    Location reverseGeocode(
            @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być ≥ -90")
            @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być ≤ 90")
            double latitude,

            @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być ≥ -180")
            @DecimalMax(value = "180.0", message = "Długość geograficzna musi być ≤ 180")
            double longitude
    );

    boolean validateAddress(
            @NotNull(message = "Lokalizacja nie może być pusta")
            Location location
    );

    String getTimezone(
            @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być ≥ -90")
            @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być ≤ 90")
            double latitude,

            @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być ≥ -180")
            @DecimalMax(value = "180.0", message = "Długość geograficzna musi być ≤ 180")
            double longitude
    );
}
