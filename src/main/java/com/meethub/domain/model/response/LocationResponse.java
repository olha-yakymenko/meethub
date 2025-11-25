package com.meethub.domain.model.response;

import com.meethub.domain.model.enums.LocationType;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {
    private Long id;
    private String name;
    private LocationType type;

    // Physical location fields
    private String address;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Virtual location fields
    private String virtualMeetingUrl;
    private String accessCode;

    // Additional info
    private String drivingInstructions;
    private String timezone;
    private String mapUrl;
    private String directionsUrl;

    public String getFullAddress() {
        if (this.type == LocationType.VIRTUAL) {
            return this.virtualMeetingUrl != null ? this.virtualMeetingUrl : "Brak URL";
        }

        // Dla lokalizacji fizycznej
        List<String> parts = new ArrayList<>();
        if (this.address != null && !this.address.trim().isEmpty()) {
            parts.add(this.address);
        }
        if (this.city != null && !this.city.trim().isEmpty()) {
            parts.add(this.city);
        }
        if (this.country != null && !this.country.trim().isEmpty()) {
            parts.add(this.country);
        }

        return parts.isEmpty() ? "Brak adresu" : String.join(", ", parts);
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}