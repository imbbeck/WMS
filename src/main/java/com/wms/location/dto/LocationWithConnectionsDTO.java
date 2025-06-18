package com.wms.location.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.wms.location.domain.model.LocationType;

public record LocationWithConnectionsDTO(Long id, String name, LocationType type, Integer capacity, LocalDateTime createdAt, LocalDateTime updatedAt, List<LocationConnectionDTO.Res> connections) {
}
