package com.wms.location.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.wms.location.application.LocationConnectionService;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.dto.LocationConnectionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/location-connections")
@Tag(name = "Location Connection Management", description = "APIs for managing connections between warehouse locations")
public class LocationConnectionController {

	private final LocationConnectionService connectionService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create location connection", description = "Creates a new connection between two warehouse locations")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Connection created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public LocationConnectionDTO.Res createConnection(@Valid @RequestBody LocationConnectionDTO.CreateReq request) {
		LocationConnection connection = connectionService.createConnection(request);
		return LocationConnectionDTO.Res.builder()
				.locationAId(connection.getLocationAId())
				.locationBId(connection.getLocationBId())
				.build();
	}

	@GetMapping
	@Operation(summary = "Get all location connections", description = "Retrieves all connections between warehouse locations")
	@ApiResponse(responseCode = "200", description = "Successfully retrieved connections")
	public List<LocationConnectionDTO.Res> getAllConnections() {
		return connectionService.getAllConnections().stream()
				.map(LocationConnectionDTO.Res::from)
				.collect(Collectors.toList());
	}

	@GetMapping("/by-location/{locationId}")
	@Operation(summary = "Get connections by location", description = "Retrieves all connections for a specific location")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved connections"),
			@ApiResponse(responseCode = "404", description = "Location not found")
	})
	public List<LocationConnectionDTO.ConnectionInfo> getConnectionsByLocation(
			@Parameter(description = "Location ID") @PathVariable Long locationId) {
		List<LocationConnection> connections = connectionService.getConnectionsByLocationId(locationId);
		if (connections.isEmpty()) {
			return new ArrayList<>();
		}
		return connections.stream()
				.map(conn -> {
					Long connectedLocationId = conn.getOtherLocationId(locationId);

					return LocationConnectionDTO.ConnectionInfo.builder()
							.connectionId(conn.getId())
							.connectedLocationId(connectedLocationId)
							.trt(conn.getTrt())
							.build();
				})
				.collect(Collectors.toList());
	}

	@PutMapping("/{connectionId}")
	@Operation(summary = "Update location connection", description = "Updates an existing connection between locations (e.g., travel time)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Connection updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data"),
			@ApiResponse(responseCode = "404", description = "Connection not found")
	})
	public LocationConnectionDTO.Res updateConnection(
			@Parameter(description = "Connection ID") @PathVariable Long connectionId,
			@Valid @RequestBody LocationConnectionDTO.UpdateReq request) {
		LocationConnection connection = connectionService.updateConnection(connectionId, request);
		return LocationConnectionDTO.Res.builder()
				.locationAId(connection.getLocationAId())
				.locationBId(connection.getLocationBId())
				.build();
	}

	@DeleteMapping("/{connectionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete location connection", description = "Deletes a connection between two locations")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Connection deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Connection not found")
	})
	public void deleteConnection(@Parameter(description = "Connection ID") @PathVariable Long connectionId) {
		connectionService.deleteConnection(connectionId);
	}

}
