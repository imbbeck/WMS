package com.wms.location.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.config.TestConfig;
import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.location.application.LocationService;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.dto.LocationConnectionDTO;
import com.wms.location.dto.LocationDTO;
import com.wms.location.dto.LocationWithConnectionsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestSecurityConfig.class, TestConfig.class})
@WebMvcTest(LocationController.class)
class LocationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LocationService locationService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private DomainCacheManager<Long, String> domainCacheManager;

	@Test
	@DisplayName("POST /locations - 장소 생성 요청을 성공하고 201 Created를 반환한다.")
	void createLocation_Success() throws Exception {
		// given
		var requestDto = LocationDTO.CreateReq.builder()
				.name("창고A")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.coordinateX(100)
				.coordinateY(100)
				.build();
		var mockLocation = Location.builder()
				.name("창고A")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.coordinateX(100)
				.coordinateY(100)
				.build();

		given(locationService.createLocation(any(LocationDTO.CreateReq.class))).willReturn(mockLocation);

		// when & then
		mockMvc.perform(post("/locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("창고A"));
	}

	@Test
	@DisplayName("POST /locations - 유효성 검증 실패 시 400 Bad Request를 반환한다.")
	void createLocation_WithInvalidInput_ReturnsBadRequest() throws Exception {
		// given
		var invalidRequestDto = new LocationDTO.CreateReq("", null, null, null, null); // 이름, 타입이 비어있음

		// when & then
		mockMvc.perform(post("/locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequestDto)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("GET /locations - 모든 장소 목록을 조회하고 200 OK를 반환한다.")
	void getLocations_Success() throws Exception {
		// given
		var mockLocation1 = Location.builder()
				.name("창고A")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.coordinateX(100)
				.coordinateY(100)
				.build();
		var mockLocation2 = Location.builder()
				.name("입고처A")
				.type(LocationType.INBOUND)
				.coordinateX(100)
				.coordinateY(100)
				.build();

		var locationList = List.of(mockLocation1, mockLocation2);
		given(locationService.getAllLocations()).willReturn(locationList);

		// when & then
		mockMvc.perform(get("/locations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("창고A"))
				.andExpect(jsonPath("$[0].capacity").value(100))
				.andExpect(jsonPath("$[1].name").value("입고처A"))
				.andExpect(jsonPath("$[1].capacity").doesNotExist()) // null 대신 doesNotExist() 사용
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	@DisplayName("GET /locations/type/{type} - 특정 타입의 장소 목록을 조회하고 200 OK를 반환한다.")
	void getLocationsByType_Success() throws Exception {
		// Given
		LocationType type = LocationType.WAREHOUSE;
		List<Location> locations = Arrays.asList(
				Location.builder()
						.name("창고A")
						.type(LocationType.WAREHOUSE)
						.capacity(100)
						.coordinateX(100)
						.coordinateY(100)
						.build(),
				Location.builder()
						.name("창고B")
						.type(LocationType.WAREHOUSE)
						.capacity(200)
						.coordinateX(200)
						.coordinateY(200)
						.build()
		);

		given(locationService.getLocationsByType(LocationType.WAREHOUSE)).willReturn(locations);

		// when & then
		mockMvc.perform(get("/locations/type/{type}", LocationType.WAREHOUSE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("창고A"))
				.andExpect(jsonPath("$[0].type").value("WAREHOUSE"))
				.andExpect(jsonPath("$[0].capacity").value(100))
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	@DisplayName("GET /locations/{id} - 특정 장소와 연결 정보를 조회하고 200 OK를 반환한다.")
	void getLocation_Success() throws Exception {
		// given
		Location locA = Location.builder()
				.name("창고A")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.coordinateX(100)
				.coordinateY(100) // coordinateX가 중복되어 있었음
				.build();
		Location locB = Location.builder()
				.name("창고B")
				.type(LocationType.WAREHOUSE)
				.capacity(100)
				.coordinateX(200)
				.coordinateY(200)
				.build();
		Location locC = Location.builder()
				.name("출고처")
				.type(LocationType.OUTBOUND)
				.capacity(null)
				.coordinateX(300)
				.coordinateY(300)
				.build();

		LocationConnectionDTO.Res locationConnectionAandB = LocationConnectionDTO.Res.builder()
				.locationAId(1L)
				.locationBId(2L)
				.trt(50)
				.build();
		LocationConnectionDTO.Res locationConnectionAandC = LocationConnectionDTO.Res.builder()
				.locationAId(1L)
				.locationBId(3L)
				.trt(20)
				.build();

		var dto = new LocationWithConnectionsDTO(
				1L,
				"창고A",
				LocationType.WAREHOUSE,
				100,
				LocalDateTime.now(),
				LocalDateTime.now(),
				List.of(locationConnectionAandB, locationConnectionAandC)
		);
		given(locationService.getLocationWithConnections(1L)).willReturn(dto);


		// when & then
		mockMvc.perform(get("/locations/{id}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("창고A"))
				.andExpect(jsonPath("$.connections").isArray())
				.andExpect(jsonPath("$.connections.length()").value(2));
	}



	@Test
	@DisplayName("GET /locations/{id} - 존재하지 않는 장소 ID로 조회 시 404 Not Found를 반환해야 한다.")
	void getLocation_NotFound_ShouldReturnNotFound() throws Exception {
		// given
		// ControllerAdvice가 예외를 처리하여 404를 반환한다고 가정
		given(locationService.getLocationWithConnections(99L)).willThrow(LocationException.notFound(99L));

		// when & then
		mockMvc.perform(get("/locations/{id}", 99L))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PUT /locations/{id} - 장소 정보를 수정하고 200 OK를 반환한다.")
	void updateLocation_Success() throws Exception {
		// given
		var requestDto = LocationDTO.UpdateReq.builder()
				.name("수정된 창고")
				.capacity(200)
				.coordinateX(100)
				.coordinateY(100)
				.build();

		var updatedLocation = Location.builder()
				.name("수정된 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(200)
				.coordinateX(100)
				.coordinateY(100)
				.build(); //


		given(locationService.updateLocation(eq(1L), any(LocationDTO.UpdateReq.class))).willReturn(updatedLocation);

		// when & then
		mockMvc.perform(put("/locations/{id}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("수정된 창고"))
				.andExpect(jsonPath("$.capacity").value(200));
	}

	@Test
	@DisplayName("DELETE /locations/{id} - 장소를 삭제하고 204 No Content를 반환한다.")
	void deleteLocation_Success() throws Exception {
		// given
		doNothing().when(locationService).deleteLocation(1L);

		// when & then
		mockMvc.perform(delete("/locations/{id}", 1L))
				.andExpect(status().isNoContent());
	}
}