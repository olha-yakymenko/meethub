//package com.meethub.domain.model.request;
//
//import com.meethub.domain.model.enums.LocationType;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//public class UpdateLocationRequest {
//
//    @NotBlank(message = "Nazwa lokalizacji jest wymagana")
//    private String name;
//
//    @NotNull(message = "Typ lokalizacji jest wymagany")
//    private LocationType type;
//
//    private String address;
//    private String city;
//    private String country;
//    private BigDecimal latitude;
//    private BigDecimal longitude;
//    private String virtualMeetingUrl;
//    private String accessCode;
//    private String drivingInstructions;
//    private String timezone;
//}





package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.LocationType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateLocationRequest {

    @NotBlank(message = "Nazwa lokalizacji jest wymagana")
    @Size(min = 2, max = 200, message = "Nazwa musi mieć 2-200 znaków")
    private String name;

    @NotNull(message = "Typ lokalizacji jest wymagany")
    private LocationType type;

    @Size(max = 1000, message = "Adres nie może przekraczać 1000 znaków")
    private String address;

    @Size(max = 100, message = "Miasto nie może przekraczać 100 znaków")
    private String city;

    @Size(max = 100, message = "Kraj nie może przekraczać 100 znaków")
    private String country;

    @DecimalMin(value = "-90.000000", message = "Szerokość geograficzna musi być między -90 a 90")
    @DecimalMax(value = "90.000000", message = "Szerokość geograficzna musi być między -90 a 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.000000", message = "Długość geograficzna musi być między -180 a 180")
    @DecimalMax(value = "180.000000", message = "Długość geograficzna musi być między -180 a 180")
    private BigDecimal longitude;

    @Size(max = 500, message = "URL spotkania nie może przekraczać 500 znaków")
    @URL(message = "Invalid URL format")
    private String virtualMeetingUrl;

    @Size(max = 50, message = "Kod dostępu nie może przekraczać 50 znaków")
    private String accessCode;

    @Size(max = 2000, message = "Instrukcje dojazdu nie mogą przekraczać 2000 znaków")
    private String drivingInstructions;

    @Pattern(regexp = "^[A-Za-z_]+/[A-Za-z_]+$",
            message = "Strefa czasowa musi być w formacie: Kontynent/Miasto")
    @Size(max = 50, message = "Strefa czasowa nie może przekraczać 50 znaków")
    private String timezone;

    // Walidacja biznesowa
    @AssertTrue(message = "Virtual meeting URL is required for virtual locations")
    public boolean isVirtualUrlValid() {
        if (type == LocationType.VIRTUAL) {
            return virtualMeetingUrl != null && !virtualMeetingUrl.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "Address is required for physical locations")
    public boolean isAddressValid() {
        if (type == LocationType.PHYSICAL) {
            return address != null && !address.isBlank();
        }
        return true;
    }
}