package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.LocationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "meetings")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    @Column(name = "virtual_meeting_url")
    private String virtualMeetingUrl;

    @Column(name = "access_code", length = 50)
    private String accessCode;

    @Column(name = "driving_instructions", columnDefinition = "TEXT")
    private String drivingInstructions;

    @Column(name = "timezone", length = 50)
    private String timezone;

    // Relacja z Meeting (opcjonalnie)
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Meeting> meetings = new ArrayList<>();

    public Location(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        this.id = location.id;
        this.name = location.name;
        this.address = location.address;
        this.city = location.city;
        this.country = location.country;
        this.latitude = location.latitude;
        this.longitude = location.longitude;
        this.type = location.type;
        this.virtualMeetingUrl = location.virtualMeetingUrl;
        this.accessCode = location.accessCode;
        this.drivingInstructions = location.drivingInstructions;
        this.timezone = location.timezone;
        // meetings nie kopiujemy - to jest lista spotkań, powinna pozostać pusta w kopii
        this.meetings = new ArrayList<>();
    }

    // Metody pomocnicze
    public String getFullAddress() {
        if (type == LocationType.VIRTUAL) {
            return virtualMeetingUrl;
        }
        if (address != null && city != null) {
            return String.format("%s, %s, %s", address, city, country != null ? country : "");
        }
        return address;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public boolean isVirtual() {
        return type == LocationType.VIRTUAL;
    }

    public boolean isPhysical() {
        return type == LocationType.PHYSICAL;
    }
}