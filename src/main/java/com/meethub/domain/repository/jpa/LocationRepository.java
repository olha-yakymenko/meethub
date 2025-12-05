package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    // Podstawowe wyszukiwanie
    List<Location> findByType(LocationType type);
    List<Location> findByCity(String city);
    List<Location> findByCountry(String country);
    Page<Location> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // ✅ POPRAWIONE: Wyszukiwanie zaawansowane - użyj nativeQuery
    @Query(value = """
        SELECT l.* FROM meethub_schema.locations l 
        WHERE 
            (:query IS NULL OR 
             LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR 
             LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR 
             LOWER(l.address) LIKE LOWER(CONCAT('%', :query, '%'))) 
        AND 
            (:type IS NULL OR l.type = :type) 
        AND 
            (:city IS NULL OR LOWER(l.city) = LOWER(:city))
        """,
            nativeQuery = true,
            countQuery = """
        SELECT COUNT(*) FROM meethub_schema.locations l 
        WHERE 
            (:query IS NULL OR 
             LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR 
             LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR 
             LOWER(l.address) LIKE LOWER(CONCAT('%', :query, '%'))) 
        AND 
            (:type IS NULL OR l.type = :type) 
        AND 
            (:city IS NULL OR LOWER(l.city) = LOWER(:city))
        """)
    Page<Location> searchLocations(@Param("query") String query,
                                   @Param("type") String type,  // ✅ Zmieniono na String dla nativeQuery
                                   @Param("city") String city,
                                   Pageable pageable);

    // ✅ POPRAWIONE: Wyszukiwanie w pobliżu (Haversine formula)
    @Query(value = """
    SELECT * FROM meethub_schema.locations 
    WHERE latitude IS NOT NULL 
    AND longitude IS NOT NULL
    AND type = 'PHYSICAL'
    AND (
        6371 * 
        acos(
            greatest(-1, least(1, 
                cos(radians(:lat)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(latitude))
            ))
        )
    ) < :radius
    """,
    nativeQuery = true)
    List<Location> findNearbyLocations(@Param("lat") BigDecimal latitude,
                                       @Param("lng") BigDecimal longitude,
                                       @Param("radius") Double radiusKm);

    // Lokalizacje z koordynatami
    @Query("SELECT l FROM Location l WHERE l.latitude IS NOT NULL AND l.longitude IS NOT NULL")
    List<Location> findLocationsWithCoordinates();

    // Sprawdzanie unikalności
    Optional<Location> findByVirtualMeetingUrl(String virtualMeetingUrl);
    boolean existsByNameAndAddress(String name, String address);
}