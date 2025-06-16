package com.wms.location.domain.repository;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByType(LocationType type);
    boolean existsByName(String name);
} 