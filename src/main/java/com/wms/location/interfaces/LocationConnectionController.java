package com.wms.location.interfaces;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.wms.Infra.domain.ReferenceDataCacheManager;
import com.wms.location.application.LocationConnectionService;
import com.wms.location.domain.model.LocationConnection;
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

	// 연결 생성
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LocationConnectionDTO.Res createConnection(@Valid @RequestBody LocationConnectionDTO.CreateReq request) {
		LocationConnection connection = connectionService.createConnection(request);
		return LocationConnectionDTO.Res.builder()
				.locationAId(connection.getLocationAId())
				.locationBId(connection.getLocationBId())
				.build();
	}

	// 모든 연결 조회(개별 연결 조회는 필요없음. 연결조회는 비지니스로직상 장소와 같이 조회되므로)
	@GetMapping
	public List<LocationConnectionDTO.Res> getAllConnections() {
		return connectionService.getAllConnections().stream()
				.map(LocationConnectionDTO.Res::from)
				.collect(Collectors.toList());
	}

	// 특정 Location의 연결 정보 조회
	@GetMapping("/by-location/{locationId}")
	public List<LocationConnectionDTO.ConnectionInfo> getConnectionsByLocation(@PathVariable Long locationId) {
		List<LocationConnection> connections = connectionService.getConnectionsByLocationId(locationId);

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

	// 연결 수정 (이동 시간 변경)
	@PutMapping("/{connectionId}")
	public LocationConnectionDTO.Res updateConnection(
			@PathVariable Long connectionId,
			@Valid @RequestBody LocationConnectionDTO.UpdateReq request) {
		LocationConnection connection = connectionService.updateConnection(connectionId, request);
		return LocationConnectionDTO.Res.builder()
				.locationAId(connection.getLocationAId())
				.locationBId(connection.getLocationBId())
				.build();
	}

	// 연결 삭제
	@DeleteMapping("/{connectionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteConnection(@PathVariable Long connectionId) {
		connectionService.deleteConnection(connectionId);
	}

	private final ReferenceDataCacheManager cache;

	@GetMapping("/id_name_pair")
	@ResponseStatus(HttpStatus.OK)
	public Map<Long, String> getIdNamePair() {
		return  cache.getLocationCache();
	}
}
