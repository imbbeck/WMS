package com.wms.location.domain.repository;

import java.util.Optional;

import com.wms.location.dto.LocationWithConnectionsDTO;

public interface LocationRepositoryCustom {
	Optional<LocationWithConnectionsDTO> findLocationWithConnections(Long id);
}
