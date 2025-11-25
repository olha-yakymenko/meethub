package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLocationRequest {

    @NotBlank(message = "Nazwa lokalizacji jest wymagana")
    private String name;

    @NotNull(message = "Typ lokalizacji jest wymagany")
    private LocationType type;

    private String address;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String virtualMeetingUrl;
    private String accessCode;
    private String drivingInstructions;
    private String timezone;
}