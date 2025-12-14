package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.projection.LocationBasicInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    private Location physicalLocation;
    private Location virtualLocation;

    @BeforeEach
    void setUp() {
        physicalLocation = Location.builder()
                .name("Physical Hall")
                .address("123 Main St")
                .city("CityA")
                .type(LocationType.PHYSICAL)
                .latitude(new BigDecimal("50.061"))
                .longitude(new BigDecimal("19.937"))
                .build();

        virtualLocation = Location.builder()
                .name("Virtual Room")
                .virtualMeetingUrl("https://meet.example.com/room")
                .type(LocationType.VIRTUAL)
                .build();

        locationRepository.save(physicalLocation);
        locationRepository.save(virtualLocation);
    }

    @Test
    @DisplayName("should find location by id")
    void shouldFindById() {
        Optional<Location> found = locationRepository.findById(physicalLocation.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Physical Hall");
    }

    @Test
    @DisplayName("should check existence by name and address")
    void shouldExistByNameAndAddress() {
        boolean exists = locationRepository.existsByNameAndAddress("Physical Hall", "123 Main St");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("should find location by virtual meeting url")
    void shouldFindByVirtualMeetingUrl() {
        Optional<Location> found = locationRepository.findByVirtualMeetingUrl("https://meet.example.com/room");
        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(LocationType.VIRTUAL);
    }

    @Test
    @DisplayName("should search locations with query")
    void shouldSearchLocations() {
        Page<Location> page = locationRepository.searchLocations("hall", null, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Physical Hall");
    }

    @Test
    @DisplayName("should find nearby physical locations")
    void shouldFindNearbyLocations() {
        List<Location> nearby = locationRepository.findNearbyLocations(
                new BigDecimal("50.061"),
                new BigDecimal("19.937"),
                5.0
        );
        assertThat(nearby).contains(physicalLocation);
        assertThat(nearby).doesNotContain(virtualLocation);
    }

    @Test
    @DisplayName("should return basic info projection")
    void shouldReturnAllBasicInfo() {
        List<LocationBasicInfo> basicInfo = locationRepository.findAllBasicInfo();
        assertThat(basicInfo).hasSize(3);
        assertThat(basicInfo.get(0).getName()).isNotNull();
    }

    @Test
    @DisplayName("should return locations for select projection")
    void shouldReturnAllForSelect() {
        List<LocationBasicInfo> selectInfo = locationRepository.findAllForSelect();
        assertThat(selectInfo).hasSize(3);
        assertThat(selectInfo.get(0).getId()).isNotNull();
    }
}
