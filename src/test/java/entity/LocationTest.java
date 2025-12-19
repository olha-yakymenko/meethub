package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.LocationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class LocationTest {

    @Test
    void shouldCreateLocationWithBuilder() {
        // When
        Location location = Location.builder()
                .name("Google Meet")
                .address("1600 Amphitheatre Parkway")
                .city("Mountain View")
                .country("USA")
                .latitude(new BigDecimal("37.4220"))
                .longitude(new BigDecimal("-122.0841"))
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://meet.google.com/abc-xyz")
                .accessCode("123456")
                .timezone("America/Los_Angeles")
                .build();

        // Then
        assertAll(
                () -> assertThat(location).isNotNull(),
                () -> assertThat(location.getName()).isEqualTo("Google Meet"),
                () -> assertThat(location.getAddress()).isEqualTo("1600 Amphitheatre Parkway"),
                () -> assertThat(location.getCity()).isEqualTo("Mountain View"),
                () -> assertThat(location.getCountry()).isEqualTo("USA"),
                () -> assertThat(location.getLatitude()).isEqualTo(new BigDecimal("37.4220")),
                () -> assertThat(location.getLongitude()).isEqualTo(new BigDecimal("-122.0841")),
                () -> assertThat(location.getType()).isEqualTo(LocationType.VIRTUAL),
                () -> assertThat(location.getVirtualMeetingUrl()).isEqualTo("https://meet.google.com/abc-xyz"),
                () -> assertThat(location.getAccessCode()).isEqualTo("123456"),
                () -> assertThat(location.getTimezone()).isEqualTo("America/Los_Angeles")
        );
    }

    @Test
    void shouldGetFullAddressForPhysicalLocation() {
        // Given
        Location location = Location.builder()
                .address("123 Main St")
                .city("Warsaw")
                .country("Poland")
                .type(LocationType.PHYSICAL)
                .build();

        // When
        String fullAddress = location.getFullAddress();

        // Then
        assertAll(
                () -> assertThat(fullAddress).isEqualTo("123 Main St, Warsaw, Poland")
        );
    }

    @Test
    void shouldGetVirtualMeetingUrlForVirtualLocation() {
        // Given
        Location location = Location.builder()
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456")
                .build();

        // When
        String fullAddress = location.getFullAddress();

        // Then
        assertAll(
                () -> assertThat(fullAddress).isEqualTo("https://zoom.us/j/123456")
        );
    }

    @Test
    void shouldCheckIfHasCoordinates() {
        // Given
        Location locationWithCoords = Location.builder()
                .latitude(new BigDecimal("52.2297"))
                .longitude(new BigDecimal("21.0122"))
                .build();

        Location locationWithoutCoords = Location.builder().build();

        // Then
        assertAll(
                () -> assertThat(locationWithCoords.hasCoordinates()).isTrue(),
                () -> assertThat(locationWithoutCoords.hasCoordinates()).isFalse()
        );
    }

    @Test
    void shouldCheckLocationType() {
        // Given
        Location virtualLocation = Location.builder()
                .type(LocationType.VIRTUAL)
                .build();

        Location physicalLocation = Location.builder()
                .type(LocationType.PHYSICAL)
                .build();

        // Then
        assertAll(
                () -> assertThat(virtualLocation.isVirtual()).isTrue(),
                () -> assertThat(virtualLocation.isPhysical()).isFalse(),
                () -> assertThat(physicalLocation.isPhysical()).isTrue(),
                () -> assertThat(physicalLocation.isVirtual()).isFalse()
        );
    }

    @Test
    void shouldCreateCopyConstructor() {
        // Given
        Location original = Location.builder()
                .id(1L)
                .name("Original")
                .address("Test Address")
                .city("Test City")
                .country("Test Country")
                .type(LocationType.PHYSICAL)
                .build();

        // When
        Location copy = new Location(original);

        // Then
        assertAll(
                () -> assertThat(copy.getId()).isEqualTo(1L),
                () -> assertThat(copy.getName()).isEqualTo("Original"),
                () -> assertThat(copy.getAddress()).isEqualTo("Test Address"),
                () -> assertThat(copy.getCity()).isEqualTo("Test City"),
                () -> assertThat(copy.getCountry()).isEqualTo("Test Country"),
                () -> assertThat(copy.getType()).isEqualTo(LocationType.PHYSICAL),
                () -> assertThat(copy.getMeetings()).isEmpty() // Meetings list should be new
        );
    }
}