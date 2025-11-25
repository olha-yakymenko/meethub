package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLocationRequest {

    public interface PhysicalValidation {}
    public interface VirtualValidation {}

    @NotBlank(message = "Nazwa lokalizacji jest wymagana")
    private String name;

    @NotNull(message = "Typ lokalizacji jest wymagany")
    private LocationType type;

    // Wymagane tylko dla lokalizacji fizycznej
    @NotBlank(message = "Adres jest wymagany dla lokalizacji fizycznej", groups = PhysicalValidation.class)
    private String address;

    @NotBlank(message = "Miasto jest wymagane dla lokalizacji fizycznej", groups = PhysicalValidation.class)
    private String city;

    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String drivingInstructions;

    // Wymagane tylko dla lokalizacji wirtualnej
    @NotBlank(message = "URL spotkania jest wymagany dla lokalizacji wirtualnej", groups = VirtualValidation.class)
    private String virtualMeetingUrl;

    private String accessCode;
    private String timezone;
}