package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchRequest {
    private String query;
    private LocationType type;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double radiusKm;
    private Integer page = 0;
    private Integer size = 20;
}