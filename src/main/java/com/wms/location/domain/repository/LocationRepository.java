package com.wms.location.domain.repository;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // Location과 연결된 모든 Connection 정보 함께 조회
    @Query("SELECT l FROM Location l " +
            "LEFT JOIN FETCH l.connectionsAsA " +
            "LEFT JOIN FETCH l.connectionsAsB " +
            "WHERE l.id = :id")
    Optional<Location> findByIdWithAllConnections(@Param("id") Long id);

    List<Location> findByType(LocationType type);

    boolean existsByName(String name);
} 