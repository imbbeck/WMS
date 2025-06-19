package com.wms.location.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.wms.location.domain.model.LocationType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Location with its connections")
public record LocationWithConnectionsDTO(
		@Schema(description = "Location ID", example = "1")
		Long id,

		@Schema(description = "Location name", example = "Warehouse A")
		String name,

		@Schema(description = "Location type", example = "WAREHOUSE")
		LocationType type,

		@Schema(description = "Storage capacity", example = "1000")
		Integer capacity,

		@Schema(description = "Creation timestamp")
		LocalDateTime createdAt,

		@Schema(description = "Last update timestamp")
		LocalDateTime updatedAt,

		@Schema(description = "List of connections to other locations")
		List<LocationConnectionDTO.Res> connections
) {
}
