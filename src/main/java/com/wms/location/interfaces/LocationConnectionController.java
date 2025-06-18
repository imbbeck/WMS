package com.wms.location.interfaces;

import java.util.List;
import java.util.stream.Collectors;

import com.wms.location.application.LocationConnectionService;
import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.repository.LocationCacheManager;
import com.wms.location.dto.LocationConnectionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/location-connections")
public class LocationConnectionController {

	private final LocationConnectionService connectionService;
	private final LocationCacheManager locationCache;

	// 연결 생성
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LocationConnectionDTO.Res createConnection(@Valid @RequestBody LocationConnectionDTO.CreateReq request) {
		LocationConnection connection = connectionService.createConnection(request);
		return LocationConnectionDTO.Res.builder()
				.connection(connection)
				.locationCache(locationCache)
				.build();
	}

	// 연결 상세 조회
	@GetMapping("/{connectionId}")
	public LocationConnectionDTO.Res getConnection(@PathVariable Long connectionId) {
		LocationConnection connection = connectionService.getConnection(connectionId);
		return LocationConnectionDTO.Res.builder()
				.connection(connection)
				.locationCache(locationCache)
				.build();
	}

	// 모든 연결 조회 (간단 정보)
	@GetMapping
	public List<LocationConnectionDTO.SimpleRes> getAllConnections() {
		return connectionService.getAllConnections().stream()
				.map(LocationConnectionDTO.SimpleRes::new)
				.collect(Collectors.toList());
	}

	// 특정 Location의 연결 정보 조회
	@GetMapping("/by-location/{locationId}")
	public List<LocationConnectionDTO.ConnectionInfo> getConnectionsByLocation(@PathVariable Long locationId) {
		List<LocationConnection> connections = connectionService.getConnectionsByLocationId(locationId);

		return connections.stream()
				.map(conn -> {
					Long connectedLocationId = conn.getOtherLocationId(locationId);
					String connectedLocationName = locationCache.getName(connectedLocationId);

					return LocationConnectionDTO.ConnectionInfo.builder()
							.connectionId(conn.getId())
							.connectedLocationId(connectedLocationId)
							.connectedLocationName(connectedLocationName)
							.trt(conn.getTrt())
							.build();
				})
				.collect(Collectors.toList());
	}

	// 연결 수정 (이동 시간 변경)
	@PutMapping("/{connectionId}")
	public LocationConnectionDTO.Res updateConnection(
			@PathVariable Long connectionId,
			@Valid @RequestBody LocationConnectionDTO.UpdateReq request) {
		LocationConnection connection = connectionService.updateConnection(connectionId, request);
		return LocationConnectionDTO.Res.builder()
				.connection(connection)
				.locationCache(locationCache)
				.build();
	}

	// 연결 삭제
	@DeleteMapping("/{connectionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteConnection(@PathVariable Long connectionId) {
		connectionService.deleteConnection(connectionId);
	}

	// 두 Location 간 연결 조회
	@GetMapping("/between")
	public LocationConnectionDTO.Res getConnectionBetweenLocations(
			@RequestParam Long locationId1,
			@RequestParam Long locationId2) {
		LocationConnection connection = connectionService.getConnectionByLocations(locationId1, locationId2);
		return LocationConnectionDTO.Res.builder()
				.connection(connection)
				.locationCache(locationCache)
				.build();
	}

	// 특정 Location의 연결 개수 조회
	@GetMapping("/count/{locationId}")
	public Long getConnectionCount(@PathVariable Long locationId) {
		return connectionService.getConnectionCount(locationId);
	}

	// 🚨 연결 존재 여부 확인
	@GetMapping("/exists")
	public Boolean isConnected(@RequestParam Long locationId1, @RequestParam Long locationId2) {
		return connectionService.isConnected(locationId1, locationId2);
	}

	// 여러 Location과 관련된 연결들 조회
	@GetMapping("/by-locations")
	public List<LocationConnectionDTO.SimpleRes> getConnectionsByLocationIds(
			@RequestParam List<Long> locationIds) {
		return connectionService.getConnectionsByLocationIds(locationIds).stream()
				.map(LocationConnectionDTO.SimpleRes::new)
				.collect(Collectors.toList());
	}

}
