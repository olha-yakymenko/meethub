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

    @Override
    List<Location> findAll();

    @Override
    Page<Location> findAll(Pageable pageable);

    @Override
    Optional<Location> findById(Long id);

    @Override
    boolean existsById(Long id);
    boolean existsByNameAndAddress(String name, String address);


    Optional<Location> findByVirtualMeetingUrl(String virtualMeetingUrl);


    @Query(value = """
    SELECT * FROM meethub_schema.locations l 
    WHERE 
        (:query IS NULL OR :query = '' OR 
         LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR 
         LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR 
         LOWER(l.address) LIKE LOWER(CONCAT('%', :query, '%'))) 
    AND 
        (:type IS NULL OR :type = '' OR l.type = :type) 
    AND 
        (:city IS NULL OR :city = '' OR LOWER(l.city) = LOWER(:city))
    ORDER BY l.name
    """,
            nativeQuery = true,
            countQuery = """
    SELECT COUNT(*) FROM meethub_schema.locations l 
    WHERE 
        (:query IS NULL OR :query = '' OR 
         LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR 
         LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR 
         LOWER(l.address) LIKE LOWER(CONCAT('%', :query, '%'))) 
    AND 
        (:type IS NULL OR :type = '' OR l.type = :type) 
    AND 
        (:city IS NULL OR :city = '' OR LOWER(l.city) = LOWER(:city))
    """)
    Page<Location> searchLocations(@Param("query") String query,
                                   @Param("type") String type,
                                   @Param("city") String city,
                                   Pageable pageable);


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

    @Query("""
        SELECT l.id as id, l.name as name, l.city as city, 
               l.address as address, l.type as type 
        FROM Location l 
        ORDER BY l.name
        """)
    List<LocationBasicInfo> findAllBasicInfo();

    @Query("""
        SELECT l.id as id, l.name as name, l.city as city, l.type as type 
        FROM Location l 
        ORDER BY l.name
        """)
    List<LocationBasicInfo> findAllForSelect();

}