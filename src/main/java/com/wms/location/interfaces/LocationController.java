package com.wms.location.interfaces;

import java.util.List;

import com.wms.location.application.LocationService;
import com.wms.location.domain.model.LocationType;
import com.wms.location.dto.LocationDTO;
import com.wms.location.dto.LocationWithConnectionsDTO;
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
public class LocationController {

	private final LocationService locationService;

	@PostMapping
	@ResponseStatus(value = HttpStatus.CREATED)
	public LocationDTO.Res createLocation(@Valid @RequestBody LocationDTO.createReq request) {
		return new LocationDTO.Res(locationService.createLocation(request));
	}

	@GetMapping
	@ResponseStatus(value = HttpStatus.OK)
	public List<LocationDTO.Res> getLocations() {
		return locationService.getLocations().stream()
				.map(LocationDTO.Res::new)
				.toList();
	}

	@GetMapping("/type/{type}")
	@ResponseStatus(value = HttpStatus.OK)
	public List<LocationDTO.Res> getLocationsByType(@PathVariable LocationType type) {
		return locationService.getLocationsByType(type).stream()
				.map(LocationDTO.Res::new)
				.toList();
	}

	@GetMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	public LocationWithConnectionsDTO getLocation(@PathVariable Long id) {
		return locationService.getLocationWithConnections(id);
	}

	@PutMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	public LocationDTO.Res updateLocation(
			@PathVariable Long id,
			@Valid @RequestBody LocationDTO.updateReq request) {
		return new LocationDTO.Res(locationService.updateLocation(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	public void deleteLocation(@PathVariable Long id) {
		locationService.deleteLocation(id);
	}


} 