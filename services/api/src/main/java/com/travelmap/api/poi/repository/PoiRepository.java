package com.travelmap.api.poi.repository;

import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PoiRepository extends JpaRepository<PoiEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category", "openingHours"})
    @Query("select p from PoiEntity p where p.id = :id")
    Optional<PoiEntity> findByIdForUpdate(@Param("id") UUID id);
    @EntityGraph(attributePaths = {"category", "openingHours"})
    Optional<PoiEntity> findByIdAndStatus(UUID id, PoiStatus status);

    @EntityGraph(attributePaths = {"category", "openingHours"})
    List<PoiEntity> findAllByStatus(PoiStatus status);

    @EntityGraph(attributePaths = {"category", "openingHours"})
    List<PoiEntity> findAllByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = {"category", "openingHours"})
    List<PoiEntity> findAllByOwner_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"category", "openingHours"})
    Optional<PoiEntity> findByIdAndOwner_EmailIgnoreCase(UUID id, String email);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM poi p
                WHERE p.normalized_name = :name
                  AND ST_DWithin(
                      p.location,
                      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                      :meters,
                      false
                  )
            )
            """, nativeQuery = true)
    boolean existsDuplicateWithinMeters(@Param("name") String normalizedName,
                                        @Param("latitude") double latitude,
                                        @Param("longitude") double longitude,
                                        @Param("meters") double meters);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM poi p
                WHERE p.id <> :excludedId
                  AND p.normalized_name = :name
                  AND ST_DWithin(
                      p.location,
                      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                      :meters,
                      false
                  )
            )
            """, nativeQuery = true)
    boolean existsDuplicateWithinMetersExcluding(@Param("excludedId") UUID excludedId,
                                                 @Param("name") String normalizedName,
                                                 @Param("latitude") double latitude,
                                                 @Param("longitude") double longitude,
                                                 @Param("meters") double meters);

    @Query(value = """
            SELECT p.id AS id,
                   ST_Distance(
                       p.location,
                       ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                       false
                   ) AS distanceMeters
            FROM poi p
            WHERE p.status = 'ACTIVE'
              AND ST_DWithin(
                  p.location,
                  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                  :radiusMeters,
                  false
              )
              AND (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))
              AND (:priceLevel IS NULL OR p.price_level = :priceLevel)
            ORDER BY distanceMeters
            LIMIT :candidateLimit
            """, nativeQuery = true)
    List<SpatialCandidateProjection> findSpatialCandidates(@Param("latitude") double latitude,
                                                            @Param("longitude") double longitude,
                                                            @Param("radiusMeters") double radiusMeters,
                                                            @Param("categoryId") UUID categoryId,
                                                            @Param("priceLevel") Integer priceLevel,
                                                            @Param("candidateLimit") int candidateLimit);
}
