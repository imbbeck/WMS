package com.wms.location.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.location.application.LocationService;
import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import com.wms.location.dto.LocationDTO;
import com.wms.location.dto.LocationWithConnectionsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LocationService locationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("POST /locations - 위치 생성 요청을 성공하고 201 Created를 반환한다.")
	void createLocation_Success() throws Exception {
		// given
		var requestDto = new LocationDTO.createReq("창고A", LocationType.WAREHOUSE, 100);
		var mockLocation = new Location("창고A", LocationType.WAREHOUSE, 100);

		given(locationService.createLocation(any(LocationDTO.createReq.class))).willReturn(mockLocation);

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
		var invalidRequestDto = new LocationDTO.createReq("", null, null); // Name, Type이 비어있음

		// when & then
		mockMvc.perform(post("/locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequestDto)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("GET /locations - 모든 위치 목록을 조회하고 200 OK를 반환한다.")
	void getLocations_Success() throws Exception {
		// given
		var locationList = List.of(new Location("창고A", LocationType.WAREHOUSE, 100));
		given(locationService.getLocations()).willReturn(locationList);

		// when & then
		mockMvc.perform(get("/locations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("창고A"))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("GET /locations/type/{type} - 특정 타입의 위치 목록을 조회하고 200 OK를 반환한다.")
	void getLocationsByType_Success() throws Exception {
		// given
		var warehouseLocations = List.of(new Location("창고A", LocationType.WAREHOUSE, 100));
		given(locationService.getLocationsByType(LocationType.WAREHOUSE)).willReturn(warehouseLocations);

		// when & then
		mockMvc.perform(get("/locations/type/{type}", LocationType.WAREHOUSE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].type").value("WAREHOUSE"))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("GET /locations/{id} - 특정 위치와 연결 정보를 조회하고 200 OK를 반환한다.")
	void getLocation_Success() throws Exception {
		// given
		var dto = new LocationWithConnectionsDTO(1L, "창고A", LocationType.WAREHOUSE, 100, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList());
		given(locationService.getLocationWithConnections(1L)).willReturn(dto);

		// when & then
		mockMvc.perform(get("/locations/{id}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("창고A"))
				.andExpect(jsonPath("$.connections").isArray());
	}

	@Test
	@DisplayName("GET /locations/{id} - 존재하지 않는 위치 ID로 조회 시 404 Not Found를 반환해야 한다.")
	void getLocation_NotFound_ShouldReturnNotFound() throws Exception {
		// given
		// ControllerAdvice가 예외를 처리하여 404를 반환한다고 가정
		given(locationService.getLocationWithConnections(99L)).willThrow(new LocationException.NotFoundException(99L));

		// when & then
		mockMvc.perform(get("/locations/{id}", 99L))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PUT /locations/{id} - 위치 정보를 수정하고 200 OK를 반환한다.")
	void updateLocation_Success() throws Exception {
		// given
		var requestDto = new LocationDTO.updateReq("수정된 창고", LocationType.WAREHOUSE, 200);
		var updatedLocation = new Location("수정된 창고", LocationType.WAREHOUSE, 200);

		given(locationService.updateLocation(eq(1L), any(LocationDTO.updateReq.class))).willReturn(updatedLocation);

		// when & then
		mockMvc.perform(put("/locations/{id}", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("수정된 창고"))
				.andExpect(jsonPath("$.capacity").value(200));
	}

	@Test
	@DisplayName("DELETE /locations/{id} - 위치를 삭제하고 204 No Content를 반환한다.")
	void deleteLocation_Success() throws Exception {
		// given
		doNothing().when(locationService).deleteLocation(1L);

		// when & then
		mockMvc.perform(delete("/locations/{id}", 1L))
				.andExpect(status().isNoContent());
	}
}