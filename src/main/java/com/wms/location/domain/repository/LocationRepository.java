package com.wms.location.domain.repository;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.dto.LocationWithConnectionsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long>, LocationRepositoryCustom {

    List<Location> findByType(LocationType type);

    boolean existsByName(String name);
} 