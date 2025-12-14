package com.meethub.domain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleGeocodingService implements GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.geocoding.api-key:}")
    private String apiKey;

    @Value("${app.geocoding.enabled:false}")
    private boolean geocodingEnabled;

    @Override
    public Location geocodeAddress(String address) {

        if (!geocodingEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Geocoding disabled or API key missing");
            return createFallbackLocation(address);
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", address)
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (!"OK".equals(root.path("status").asText())) {
                return createFallbackLocation(address);
            }

            JsonNode result = root.path("results").get(0);
            JsonNode locationNode = result.path("geometry").path("location");

            double lat = locationNode.path("lat").asDouble();
            double lng = locationNode.path("lng").asDouble();

            validateCoordinates(lat, lng);

            JsonNode components = result.path("address_components");

            String street = extractAddressComponent(components, "route");
            String streetNumber = extractAddressComponent(components, "street_number");
            String city = extractAddressComponent(components, "locality");
            String postalCode = extractAddressComponent(components, "postal_code");
            String country = extractAddressComponent(components, "country");

            String fullAddress = buildFullAddress(street, streetNumber, city, postalCode, country);

            return Location.builder()
                    .name(result.path("formatted_address").asText())
                    .address(fullAddress)
                    .city(city)
                    .country(country)
                    .latitude(BigDecimal.valueOf(lat))
                    .longitude(BigDecimal.valueOf(lng))
                    .type(LocationType.PHYSICAL)
                    .timezone(getTimezone(lat, lng))
                    .build();

        } catch (Exception e) {
            log.error("Geocoding failed", e);
            return createFallbackLocation(address);
        }
    }

    @Override
    public Location reverseGeocode(double latitude, double longitude) {

        if (!geocodingEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }

        try {
            validateCoordinates(latitude, longitude);

            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("latlng", latitude + "," + longitude)
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (!"OK".equals(root.path("status").asText())) {
                return null;
            }

            JsonNode result = root.path("results").get(0);

            return Location.builder()
                    .name("Reverse Geocoded Location")
                    .address(result.path("formatted_address").asText())
                    .latitude(BigDecimal.valueOf(latitude))
                    .longitude(BigDecimal.valueOf(longitude))
                    .type(LocationType.PHYSICAL)
                    .timezone(getTimezone(latitude, longitude))
                    .build();

        } catch (Exception e) {
            log.error("Reverse geocoding failed", e);
            return null;
        }
    }

    @Override
    public boolean validateAddress(Location location) {

        if (location == null) return false;

        if (location.getType() == LocationType.VIRTUAL) {
            return isValidOnlineUrl(location.getVirtualMeetingUrl());
        }

        if (location.getAddress() == null || location.getAddress().isBlank()) {
            return false;
        }

        Location geocoded = geocodeAddress(location.getAddress());
        return geocoded.getLatitude() != null && geocoded.getLongitude() != null;
    }

    @Override
    public String getTimezone(double latitude, double longitude) {

        if (!geocodingEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            return "Europe/Warsaw";
        }

        try {
            validateCoordinates(latitude, longitude);

            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/timezone/json")
                    .queryParam("location", latitude + "," + longitude)
                    .queryParam("timestamp", System.currentTimeMillis() / 1000)
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);

            if ("OK".equals(root.path("status").asText())) {
                return root.path("timeZoneId").asText();
            }

        } catch (Exception e) {
            log.error("Timezone lookup failed", e);
        }

        return "Europe/Warsaw";
    }

    /* ======================= helpers (BEZ Bean Validation) ======================= */

    private void validateCoordinates(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng)
                || Double.isInfinite(lat) || Double.isInfinite(lng)) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
    }

    private String extractAddressComponent(JsonNode components, String type) {
        for (JsonNode component : components) {
            for (JsonNode t : component.path("types")) {
                if (type.equals(t.asText())) {
                    return component.path("long_name").asText("");
                }
            }
        }
        return "";
    }

    private String buildFullAddress(String street, String number, String city,
                                    String postalCode, String country) {

        StringBuilder sb = new StringBuilder();

        if (!street.isBlank()) {
            sb.append(street);
            if (!number.isBlank()) sb.append(" ").append(number);
        }
        if (!city.isBlank()) sb.append(", ").append(city);
        if (!postalCode.isBlank()) sb.append(", ").append(postalCode);
        if (!country.isBlank()) sb.append(", ").append(country);

        return sb.toString();
    }

    private Location createFallbackLocation(String address) {
        return Location.builder()
                .name("Custom Location")
                .address(address)
                .type(LocationType.PHYSICAL)
                .build();
    }

    private boolean isValidOnlineUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return url.startsWith("http://")
                || url.startsWith("https://")
                || url.startsWith("zoommtg://")
                || url.startsWith("teams://")
                || url.contains("meet.google.com");
    }
}







//package com.meethub.domain.service.impl;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meethub.domain.model.entity.Location;
//import com.meethub.domain.model.enums.LocationType;
//import com.meethub.domain.service.GeocodingService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.net.URI;
//import java.math.BigDecimal;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class GoogleGeocodingService implements GeocodingService {
//
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//
//    @Value("${app.geocoding.api-key:}")
//    private String apiKey;
//
//    @Value("${app.geocoding.enabled:false}")
//    private boolean geocodingEnabled;
//
//    @Override
//    public Location geocodeAddress(String address) {
//        if (!geocodingEnabled) {
//            log.warn("Geocoding is disabled");
//            return createFallbackLocation(address);
//        }
//
//        if (apiKey == null || apiKey.trim().isEmpty()) {
//            log.warn("Geocoding API key is missing");
//            return createFallbackLocation(address);
//        }
//
//        try {
//            URI uri = UriComponentsBuilder
//                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
//                    .queryParam("address", address)
//                    .queryParam("key", apiKey)
//                    .build()
//                    .toUri();
//
//            log.debug("Geocoding request: {}", uri);
//
//            String response = restTemplate.getForObject(uri, String.class);
//            JsonNode root = objectMapper.readTree(response);
//
//            String status = root.path("status").asText();
//            log.debug("Geocoding response status: {}", status);
//
//            if ("OK".equals(status)) {
//                JsonNode result = root.path("results").get(0);
//                JsonNode locationNode = result.path("geometry").path("location");
//
//                double lat = locationNode.path("lat").asDouble();
//                double lng = locationNode.path("lng").asDouble();
//
//                JsonNode addressComponents = result.path("address_components");
//                String street = extractAddressComponent(addressComponents, "route");
//                String streetNumber = extractAddressComponent(addressComponents, "street_number");
//                String city = extractAddressComponent(addressComponents, "locality");
//                String postalCode = extractAddressComponent(addressComponents, "postal_code");
//                String country = extractAddressComponent(addressComponents, "country");
//
//                // Budowanie pełnego adresu
//                String fullAddress = buildFullAddress(street, streetNumber, city, postalCode, country);
//                String formattedAddress = result.path("formatted_address").asText();
//
//                return Location.builder()
//                        .name(formattedAddress)
//                        .address(fullAddress)
//                        .city(city)
//                        .country(country)
//                        .latitude(BigDecimal.valueOf(lat))
//                        .longitude(BigDecimal.valueOf(lng))
//                        .type(LocationType.PHYSICAL)
//                        .timezone(getTimezone(lat, lng))
//                        .build();
//            } else {
//                log.warn("Geocoding failed with status: {}", status);
//                return createFallbackLocation(address);
//            }
//        } catch (Exception e) {
//            log.error("Geocoding error for address: {}", address, e);
//            return createFallbackLocation(address);
//        }
//    }
//
//    @Override
//    public Location reverseGeocode(double latitude, double longitude) {
//        if (!geocodingEnabled || apiKey == null || apiKey.trim().isEmpty()) {
//            return null;
//        }
//
//        try {
//            URI uri = UriComponentsBuilder
//                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
//                    .queryParam("latlng", latitude + "," + longitude)
//                    .queryParam("key", apiKey)
//                    .build()
//                    .toUri();
//
//            String response = restTemplate.getForObject(uri, String.class);
//            JsonNode root = objectMapper.readTree(response);
//
//            if ("OK".equals(root.path("status").asText())) {
//                JsonNode result = root.path("results").get(0);
//                String formattedAddress = result.path("formatted_address").asText();
//
//                JsonNode addressComponents = result.path("address_components");
//                String street = extractAddressComponent(addressComponents, "route");
//                String city = extractAddressComponent(addressComponents, "locality");
//                String country = extractAddressComponent(addressComponents, "country");
//
//                return Location.builder()
//                        .name("Reverse Geocoded Location")
//                        .address(formattedAddress)
//                        .city(city)
//                        .country(country)
//                        .latitude(BigDecimal.valueOf(latitude))
//                        .longitude(BigDecimal.valueOf(longitude))
//                        .type(LocationType.PHYSICAL)
//                        .timezone(getTimezone(latitude, longitude))
//                        .build();
//            }
//        } catch (Exception e) {
//            log.error("Reverse geocoding error for coordinates: {}, {}", latitude, longitude, e);
//        }
//        return null;
//    }
//
//    @Override
//    public boolean validateAddress(Location location) {
//        if (location.getType() == LocationType.VIRTUAL) {
//            return isValidOnlineUrl(location.getVirtualMeetingUrl());
//        }
//
//        try {
//            Location geocoded = geocodeAddress(location.getAddress());
//            return geocoded.getLatitude() != null && geocoded.getLongitude() != null;
//        } catch (Exception e) {
//            log.error("Address validation failed: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    public String getTimezone(double latitude, double longitude) {
//        if (!geocodingEnabled || apiKey == null || apiKey.trim().isEmpty()) {
//            return "Europe/Warsaw";
//        }
//
//        try {
//            URI uri = UriComponentsBuilder
//                    .fromHttpUrl("https://maps.googleapis.com/maps/api/timezone/json")
//                    .queryParam("location", latitude + "," + longitude)
//                    .queryParam("timestamp", System.currentTimeMillis() / 1000)
//                    .queryParam("key", apiKey)
//                    .build()
//                    .toUri();
//
//            String response = restTemplate.getForObject(uri, String.class);
//            JsonNode root = objectMapper.readTree(response);
//
//            if ("OK".equals(root.path("status").asText())) {
//                return root.path("timeZoneId").asText();
//            }
//        } catch (Exception e) {
//            log.error("Timezone lookup failed for coordinates: {}, {}", latitude, longitude, e);
//        }
//        return "Europe/Warsaw"; // Fallback
//    }
//
//    private String extractAddressComponent(JsonNode components, String type) {
//        for (JsonNode component : components) {
//            JsonNode types = component.path("types");
//            for (JsonNode t : types) {
//                if (type.equals(t.asText())) {
//                    return component.path("long_name").asText();
//                }
//            }
//        }
//        return "";
//    }
//
//    private String buildFullAddress(String street, String streetNumber, String city, String postalCode, String country) {
//        StringBuilder address = new StringBuilder();
//
//        if (street != null && !street.isEmpty()) {
//            address.append(street);
//            if (streetNumber != null && !streetNumber.isEmpty()) {
//                address.append(" ").append(streetNumber);
//            }
//        }
//
//        if (city != null && !city.isEmpty()) {
//            if (address.length() > 0) address.append(", ");
//            address.append(city);
//        }
//
//        if (postalCode != null && !postalCode.isEmpty()) {
//            if (address.length() > 0) address.append(", ");
//            address.append(postalCode);
//        }
//
//        if (country != null && !country.isEmpty()) {
//            if (address.length() > 0) address.append(", ");
//            address.append(country);
//        }
//
//        return address.toString();
//    }
//
//    private Location createFallbackLocation(String address) {
//        return Location.builder()
//                .name("Custom Location")
//                .address(address)
//                .type(LocationType.PHYSICAL)
//                .build();
//    }
//
//    private boolean isValidOnlineUrl(String url) {
//        if (url == null) return false;
//
//        return url.startsWith("http://") ||
//                url.startsWith("https://") ||
//                url.startsWith("zoommtg://") ||
//                url.startsWith("teams://") ||
//                url.startsWith("meet.google.com/") ||
//                url.contains("zoom.us/j/");
//    }
//}