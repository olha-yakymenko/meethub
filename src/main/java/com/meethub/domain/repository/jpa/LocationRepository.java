package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.projection.LocationBasicInfo;
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

    // ============ AUTOMATYCZNE METODY SPRING DATA JPA ============

    // Podstawowe metody (generowane automatycznie)
    @Override
    List<Location> findAll();

    @Override
    Page<Location> findAll(Pageable pageable);

    @Override
    Optional<Location> findById(Long id);

    @Override
    boolean existsById(Long id);

    // Filtrowanie (generowane automatycznie)
    List<Location> findByType(LocationType type);
    List<Location> findByCity(String city);
    List<Location> findByCountry(String country);
    List<Location> findByName(String name);
    List<Location> findByNameContaining(String name);
    List<Location> findByNameContainingIgnoreCase(String name);
    Page<Location> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Filtrowanie + sortowanie (generowane automatycznie)
    List<Location> findAllByOrderByNameAsc();
    List<Location> findAllByOrderByNameDesc();
    List<Location> findByTypeOrderByNameAsc(LocationType type);
    List<Location> findByCityOrderByNameAsc(String city);
    List<Location> findByCountryOrderByNameAsc(String country);

    // Sprawdzanie istnienia (generowane automatycznie)
    boolean existsByNameAndAddress(String name, String address);
    boolean existsByVirtualMeetingUrl(String virtualMeetingUrl);

    // Wyszukiwanie pojedynczego rekordu (generowane automatycznie)
    Optional<Location> findByVirtualMeetingUrl(String virtualMeetingUrl);

    // ============ CUSTOM QUERY METHODS ============

    // Lokalizacje z koordynatami
    @Query("SELECT l FROM Location l WHERE l.latitude IS NOT NULL AND l.longitude IS NOT NULL")
    List<Location> findLocationsWithCoordinates();

    // Lokalizacje po mieście (case-insensitive)
    @Query("SELECT l FROM Location l WHERE LOWER(l.city) = LOWER(:city)")
    List<Location> findByCityIgnoreCase(@Param("city") String city);

    // Lokalizacje po mieście z sortowaniem (case-insensitive)
    @Query("SELECT l FROM Location l WHERE LOWER(l.city) = LOWER(:city) ORDER BY l.name")
    List<Location> findByCityIgnoreCaseOrderByName(@Param("city") String city);

    // ============ NATIVE QUERIES (dla złożonych zapytań) ============

    // Zaawansowane wyszukiwanie
    @Query(value = """
        SELECT * FROM meethub_schema.locations l 
        WHERE 
            (:query IS NULL OR 
             LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR 
             LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR 
             LOWER(l.address) LIKE LOWER(CONCAT('%', :query, '%'))) 
        AND 
            (:type IS NULL OR l.type = :type) 
        AND 
            (:city IS NULL OR LOWER(l.city) = LOWER(:city))
        ORDER BY l.name
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
                                   @Param("type") String type,
                                   @Param("city") String city,
                                   Pageable pageable);

    // Wyszukiwanie w pobliżu (Haversine formula)
    @Query(value = """
        SELECT * FROM meethub_schema.locations 
        WHERE latitude IS NOT NULL 
        AND longitude IS NOT NULL
        AND type = 'PHYSICAL'
        AND (
            6371 * 
            acos(
                GREATEST(-1, LEAST(1, 
                    COS(RADIANS(:lat)) * COS(RADIANS(latitude)) * 
                    COS(RADIANS(longitude) - RADIANS(:lng)) + 
                    SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
                ))
            )
        ) < :radius
        ORDER BY name
        """,
            nativeQuery = true)
    List<Location> findNearbyLocations(@Param("lat") BigDecimal latitude,
                                       @Param("lng") BigDecimal longitude,
                                       @Param("radius") Double radiusKm);

    // ============ PROJECTIONS (dla wybierania określonych pól) ============

    // Podstawowe informacje dla listy
    @Query("""
        SELECT l.id as id, l.name as name, l.city as city, 
               l.address as address, l.type as type 
        FROM Location l 
        ORDER BY l.name
        """)
    List<LocationBasicInfo> findAllBasicInfo();

    // Minimalne informacje dla selecta w formularzu
    @Query("""
        SELECT l.id as id, l.name as name, l.city as city, l.type as type 
        FROM Location l 
        ORDER BY l.name
        """)
    List<LocationBasicInfo> findAllForSelect();

    // Lokalizacje fizyczne dla selecta
    @Query("""
        SELECT l.id as id, l.name as name, l.city as city 
        FROM Location l 
        WHERE l.type = 'PHYSICAL' 
        ORDER BY l.name
        """)
    List<LocationBasicInfo> findPhysicalLocationsForSelect();

    // Lokalizacje wirtualne dla selecta
    @Query("""
        SELECT l.id as id, l.name as name, l.virtualMeetingUrl as virtualMeetingUrl 
        FROM Location l 
        WHERE l.type = 'VIRTUAL' 
        ORDER BY l.name
        """)
    List<LocationBasicInfo> findVirtualLocationsForSelect();

    // ============ SPECJALNE METODY ============

    // Liczba lokalizacji według typu
    @Query("SELECT COUNT(l) FROM Location l WHERE l.type = :type")
    Long countByLocationType(@Param("type") LocationType type);

    // Czy istnieje lokalizacja z takim samym adresem
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Location l WHERE l.address = :address AND l.city = :city")
    boolean existsByAddressAndCity(@Param("address") String address,
                                   @Param("city") String city);

    // Pobierz lokalizacje bez koordynat (do geokodowania)
    @Query("SELECT l FROM Location l " +
            "WHERE l.type = 'PHYSICAL' " +
            "AND (l.latitude IS NULL OR l.longitude IS NULL) " +
            "AND l.address IS NOT NULL")
    List<Location> findLocationsWithoutCoordinates();
}