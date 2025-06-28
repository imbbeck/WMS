package com.wms.location.interfaces;

import java.util.ArrayList;
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
@Tag(name = "장소 연결 관리", description = "창고 장소 간 연결 관리 API")
public class LocationConnectionController {

	private final LocationConnectionService connectionService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "장소 연결 생성", description = "두 창고 장소 간의 새로운 연결을 생성합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "연결 생성 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
	})
	public LocationConnectionDTO.Res createConnection(@Valid @RequestBody LocationConnectionDTO.CreateReq request) {
		LocationConnection connection = connectionService.createConnection(request);
		return LocationConnectionDTO.Res.builder()
				.locationAId(connection.getLocationAId())
				.locationBId(connection.getLocationBId())
				.build();
	}

	@GetMapping
	@Operation(summary = "전체 장소 연결 조회", description = "창고 장소 간의 모든 연결을 조회합니다")
	@ApiResponse(responseCode = "200", description = "연결 목록 조회 성공")
	public List<LocationConnectionDTO.Res> getAllConnections() {
		return connectionService.getAllConnections().stream()
				.map(LocationConnectionDTO.Res::from)
				.collect(Collectors.toList());
	}

	@GetMapping("/by-location/{locationId}")
	@Operation(summary = "장소별 연결 조회", description = "특정 장소에 대한 모든 연결을 조회합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "연결 목록 조회 성공"),
			@ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
	})
	public List<LocationConnectionDTO.ConnectionInfo> getConnectionsByLocation(
			@Parameter(description = "장소 ID") @PathVariable Long locationId) {
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
	@Operation(summary = "장소 연결 수정", description = "기존 장소 간 연결을 수정합니다 (예: 이동 시간)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "연결 수정 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "404", description = "연결을 찾을 수 없음")
	})
	public LocationConnectionDTO.Res updateConnection(
			@Parameter(description = "연결 ID") @PathVariable Long connectionId,
			@Valid @RequestBody LocationConnectionDTO.UpdateReq request) {
		LocationConnection connection = connectionService.updateConnection(connectionId, request);
		return LocationConnectionDTO.Res.builder()
				.locationAId(connection.getLocationAId())
				.locationBId(connection.getLocationBId())
				.build();
	}

	@DeleteMapping("/{connectionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "장소 연결 삭제", description = "두 장소 간의 연결을 삭제합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "연결 삭제 성공"),
			@ApiResponse(responseCode = "404", description = "연결을 찾을 수 없음")
	})
	public void deleteConnection(@Parameter(description = "연결 ID") @PathVariable Long connectionId) {
		connectionService.deleteConnection(connectionId);
	}

}
