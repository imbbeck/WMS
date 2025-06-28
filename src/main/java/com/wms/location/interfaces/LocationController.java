package com.wms.location.interfaces;

import java.util.List;
import java.util.Map;

import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
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
@Tag(name = "장소 관리", description = "장소 정보 관리 API")
public class LocationController {

	private final LocationService locationService;

	@PostMapping
	@ResponseStatus(value = HttpStatus.CREATED)
	@Operation(summary = "장소 생성", description = "제공된 정보로 새로운 장소(입고처, 출고처, 창고)를 생성합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "장소 생성 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
	})
	public LocationDTO.Res createLocation(@Valid @RequestBody LocationDTO.CreateReq request) {
		return new LocationDTO.Res(locationService.createLocation(request));
	}

	@GetMapping
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "전체 장소 조회", description = "모든 장소 목록을 조회합니다")
	@ApiResponse(responseCode = "200", description = "장소 목록 조회 성공")
	public List<LocationDTO.Res> getLocations() {
		return locationService.getAllLocations().stream()
				.map(LocationDTO.Res::new)
				.toList();
	}

	@GetMapping("/type/{type}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "타입별 장소 조회", description = "특정 타입(입고처, 출고처, 창고)의 모든 장소를 조회합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "타입별 장소 조회 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 장소 타입")
	})
	public List<LocationDTO.Res> getLocationsByType(
			@Parameter(description = "필터링할 장소 타입") @PathVariable LocationType type) {
		return locationService.getLocationsByType(type).stream()
				.map(LocationDTO.Res::new)
				.toList();
	}

	@GetMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "장소 단건 조회", description = "특정 장소와 연결 정보를 조회합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "장소 조회 성공"),
			@ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
	})
	public LocationWithConnectionsDTO getLocation(
			@Parameter(description = "장소 ID") @PathVariable Long id) {
		return locationService.getLocationWithConnections(id);
	}

	@PutMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "장소 수정", description = "기존 장소의 정보를 새로운 내용으로 수정합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "장소 수정 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
	})
	public LocationDTO.Res updateLocation(
			@Parameter(description = "장소 ID") @PathVariable Long id,
			@Valid @RequestBody LocationDTO.UpdateReq request) {
		return new LocationDTO.Res(locationService.updateLocation(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	@Operation(summary = "장소 삭제", description = "창고에서 장소를 삭제합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "장소 삭제 성공"),
			@ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
	})
	public void deleteLocation(@Parameter(description = "장소 ID") @PathVariable Long id) {
		locationService.deleteLocation(id);
	}

	private final DomainCacheManager<Long, String> locationCacheManager;

	@GetMapping("/id_name_pair")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "장소 ID-이름 쌍 조회", description = "참조용 장소 ID와 이름의 매핑 정보를 조회합니다")
	@ApiResponse(responseCode = "200", description = "장소 ID-이름 쌍 조회 성공")
	public Map<Long, String> getIdNamePair() {
		return  locationCacheManager.getIdNamePair();
	}
} 
