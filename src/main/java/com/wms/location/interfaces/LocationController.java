package com.wms.location.interfaces;

import java.util.List;
import java.util.Map;

import com.wms.infra.idnameMapCashing.DomainCacheManager;
import com.wms.location.application.LocationService;
import com.wms.location.domain.model.LocationType;
import com.wms.location.dto.LocationDTO;
import com.wms.location.dto.LocationWithConnectionsDTO;
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
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Location Management", description = "APIs for managing locations")
public class LocationController {

	private final LocationService locationService;

	@PostMapping
	@ResponseStatus(value = HttpStatus.CREATED)
	@Operation(summary = "Create a new location", description = "Creates a new location(INBOUND, OUTBOUND, WAREHOUSE) with the provided details")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Location created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public LocationDTO.Res createLocation(@Valid @RequestBody LocationDTO.CreateReq request) {
		return new LocationDTO.Res(locationService.createLocation(request));
	}

	@GetMapping
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get all locations", description = "Retrieves a list of all locations")
	@ApiResponse(responseCode = "200", description = "Successfully retrieved locations")
	public List<LocationDTO.Res> getLocations() {
		return locationService.getLocations().stream()
				.map(LocationDTO.Res::new)
				.toList();
	}

	@GetMapping("/type/{type}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get locations by type", description = "Retrieves all locations of a specific type(INBOUND, OUTBOUND, WAREHOUSE)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved locations"),
			@ApiResponse(responseCode = "400", description = "Invalid location type")
	})
	public List<LocationDTO.Res> getLocationsByType(
			@Parameter(description = "Location type to filter by") @PathVariable LocationType type) {
		return locationService.getLocationsByType(type).stream()
				.map(LocationDTO.Res::new)
				.toList();
	}

	@GetMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get location by ID", description = "Retrieves a specific location with its connections")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved location"),
			@ApiResponse(responseCode = "404", description = "Location not found")
	})
	public LocationWithConnectionsDTO getLocation(
			@Parameter(description = "Location ID") @PathVariable Long id) {
		return locationService.getLocationWithConnections(id);
	}

	@PutMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Update location", description = "Updates an existing location with new details")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Location updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data"),
			@ApiResponse(responseCode = "404", description = "Location not found")
	})
	public LocationDTO.Res updateLocation(
			@Parameter(description = "Location ID") @PathVariable Long id,
			@Valid @RequestBody LocationDTO.UpdateReq request) {
		return new LocationDTO.Res(locationService.updateLocation(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete location", description = "Deletes a location from the warehouse")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Location deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Location not found")
	})
	public void deleteLocation(@Parameter(description = "Location ID") @PathVariable Long id) {
		locationService.deleteLocation(id);
	}

	private final DomainCacheManager<Long, String> locationCacheManager;

	@GetMapping("/id_name_pair")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get location ID-name pairs", description = "Retrieves a map of location IDs to location names for reference")
	@ApiResponse(responseCode = "200", description = "Successfully retrieved location ID-name pairs")
	public Map<Long, String> getIdNamePair() {
		return  locationCacheManager.getIdNamePair();
	}
} 
