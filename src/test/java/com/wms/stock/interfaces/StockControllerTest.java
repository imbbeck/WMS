package com.wms.stock.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.applicationInfra.config.TestConfig;
import com.wms.stock.application.StockCtrlService;
import com.wms.stock.domain.exception.StockException;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.dto.StockDTO;
import com.wms.location.domain.exception.LocationException;
import com.wms.ware.domain.exception.WareException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockController.class)
@Import({TestSecurityConfig.class, TestConfig.class})
@DisplayName("StockController 웹 레이어 테스트")
class StockControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private StockCtrlService stockCtrlService;

	@Autowired
	private ObjectMapper objectMapper;

	// ========== POST /stocks 테스트 ==========

	@Test
	@DisplayName("POST /stocks - 재고 생성 성공")
	void createStock_Success() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		Stock createdStock = Stock.builder()
				.key(StockKey.of(1L, 2L)) // Mock ID 사용
				.quantity(100)
				.build();

		given(stockCtrlService.create(any(StockDTO.CreateReq.class)))
				.willReturn(createdStock);

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.wareId").value(1L))
				.andExpect(jsonPath("$.warehouseId").value(2L))
				.andExpect(jsonPath("$.quantity").value(100));

		verify(stockCtrlService).create(any(StockDTO.CreateReq.class));
	}

	@Test
	@DisplayName("POST /stocks - 유효성 검증 실패 (필수 필드 누락)")
	void createStock_ValidationFailed_MissingFields() throws Exception {
		// Given - wareId 누락
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.warehouseId(2L)
				.quantity(100)
				.build();

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verify(stockCtrlService, never()).create(any());
	}

	@Test
	@DisplayName("POST /stocks - 유효성 검증 실패 (음수 수량)")
	void createStock_ValidationFailed_NegativeQuantity() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(-10) // 음수
				.build();

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verify(stockCtrlService, never()).create(any());
	}

	@Test
	@DisplayName("POST /stocks - 존재하지 않는 물품")
	void createStock_WareNotFound() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(999L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		given(stockCtrlService.create(any(StockDTO.CreateReq.class)))
				.willThrow(WareException.notFound(999L));

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("POST /stocks - 중복 재고")
	void createStock_Duplicate() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		given(stockCtrlService.create(any(StockDTO.CreateReq.class)))
				.willThrow(new StockException.ConflictEx("이미 존재하는 재고입니다."));

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("POST /stocks - 창고 용량 초과")
	void createStock_CapacityExceeded() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(500)
				.build();

		given(stockCtrlService.create(any(StockDTO.CreateReq.class)))
				.willThrow(LocationException.warehouseCapacityExceeded(2L, 1000, 800, 500));

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());
	}

	// ========== PUT /stocks/{warehouseId}/{wareId} 테스트 ==========

	@Test
	@DisplayName("PUT /stocks/{warehouseId}/{wareId} - 재고 수정 성공")
	void updateStock_Success() throws Exception {
		// Given
		Long warehouseId = 2L;
		Long wareId = 1L;
		StockKey key = StockKey.of(wareId, warehouseId); // Mock ID 사용

		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(150)
				.build();

		Stock updatedStock = Stock.builder()
				.key(key)
				.quantity(150)
				.build();

		// Given
		given(stockCtrlService.update(eq(wareId), eq(warehouseId), any(StockDTO.UpdateReq.class)))
				.willReturn(updatedStock);

		// When & Then
		mockMvc.perform(put("/stocks/{warehouseId}/{wareId}", warehouseId, wareId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wareId").value(updatedStock.getKey().getWareId()))
				.andExpect(jsonPath("$.warehouseId").value(updatedStock.getKey().getWarehouseId()))
				.andExpect(jsonPath("$.quantity").value(updatedStock.getQuantity()));

		// 여기서도 matcher를 모두 사용
		verify(stockCtrlService).update(eq(wareId), eq(warehouseId), any(StockDTO.UpdateReq.class));
	}

	@Test
	@DisplayName("PUT /stocks/{warehouseId}/{wareId} - 재고 없음")
	void updateStock_NotFound() throws Exception {
		// Given
		Long warehouseId = 2L;
		Long wareId = 999L;
		StockKey key = StockKey.of(wareId, warehouseId); // Mock ID 사용

		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(150)
				.build();

		given(stockCtrlService.update(eq(wareId), eq(warehouseId), any(StockDTO.UpdateReq.class)))
				.willThrow(StockException.notFound(key));

		// When & Then
		mockMvc.perform(put("/stocks/{warehouseId}/{wareId}", warehouseId, wareId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PUT /stocks/{warehouseId}/{wareId} - 수량 0으로 변경 -> bad request")
	// 재고 수량이 0인 경우는 삭제로 처리되므로, PUT 요청은 허용되지 않음
	void updateStock_QuantityZero() throws Exception {
		// Given
		Long warehouseId = 2L;
		Long wareId = 1L;
		StockKey key = StockKey.of(wareId, warehouseId); // Mock ID 사용

		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(0)
				.build();

		Stock updatedStock = Stock.builder()
				.key(key)
				.quantity(0)
				.build();

		// update 호출 자체가 안 되도록 설계되었는지 확인할 것이므로 given 생략

		// When & Then
		mockMvc.perform(put("/stocks/{warehouseId}/{wareId}", warehouseId, wareId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		// update 호출이 없었는지 확인
		verify(stockCtrlService, never()).update(any(), any(), any());
	}

	@Test
	@DisplayName("PUT /stocks/{warehouseId}/{wareId} - 유효성 검증 실패")
	void updateStock_ValidationFailed() throws Exception {
		// Given
		Long warehouseId = 2L;
		Long wareId = 1L;

		StockDTO.UpdateReq request = StockDTO.UpdateReq.builder()
				.quantity(-5) // 음수
				.build();

		// When & Then
		mockMvc.perform(put("/stocks/{warehouseId}/{wareId}", warehouseId, wareId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verify(stockCtrlService, never()).update(any(), any(), any());
	}

	// ========== DELETE /stocks/{warehouseId}/{wareId} 테스트 ==========

	@Test
	@DisplayName("DELETE /stocks/{warehouseId}/{wareId} - 재고 삭제 성공")
	void deleteStock_Success() throws Exception {
		// Given
		Long warehouseId = 2L;
		Long wareId = 1L;

		willDoNothing().given(stockCtrlService).delete(wareId, warehouseId);

		// When & Then
		mockMvc.perform(delete("/stocks/{warehouseId}/{wareId}", warehouseId, wareId))
				.andExpect(status().isNoContent());

		verify(stockCtrlService).delete(wareId, warehouseId);
	}

	@Test
	@DisplayName("DELETE /stocks/{warehouseId}/{wareId} - 재고 없음")
	void deleteStock_NotFound() throws Exception {
		// Given
		Long warehouseId = 2L;
		Long wareId = 999L;
		StockKey key = StockKey.of(wareId, warehouseId); // Mock ID 사용

		willThrow(StockException.notFound(key))
				.given(stockCtrlService).delete(wareId, warehouseId);

		// When & Then
		mockMvc.perform(delete("/stocks/{warehouseId}/{wareId}", warehouseId, wareId))
				.andExpect(status().isNotFound());
	}

	// ========== 응답 형식 테스트 ==========

	@Test
	@DisplayName("성공 응답의 JSON 구조 검증 - 기본 필드만")
	void verifySuccessResponseStructure_BasicFields() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		Stock createdStock = Stock.builder()
				.key(StockKey.of(1L, 2L)) // Mock ID 사용
				.quantity(100)
				.build();

		given(stockCtrlService.create(any(StockDTO.CreateReq.class)))
				.willReturn(createdStock);

		// When & Then - BaseEntity의 필드들은 검증하지 않음
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.wareId").value(1L))
				.andExpect(jsonPath("$.warehouseId").value(2L))
				.andExpect(jsonPath("$.quantity").value(100));

	}

	@Test
	@DisplayName("성공 응답의 JSON 구조 검증 - 전체 필드")
	void verifySuccessResponseStructure_AllFields() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		// 실제 저장된 Stock 객체 시뮬레이션 (실제로는 JPA가 설정)
		Stock createdStock = createStockWithId(1L, 2L, 100, 123L, 1L);

		given(stockCtrlService.create(any(StockDTO.CreateReq.class)))
				.willReturn(createdStock);

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(123L))
				.andExpect(jsonPath("$.wareId").value(1L))
				.andExpect(jsonPath("$.warehouseId").value(2L))
				.andExpect(jsonPath("$.quantity").value(100))
				.andExpect(jsonPath("$.version").value(1L));
//				.andExpect(jsonPath("$.createdAt").exists())
//				.andExpect(jsonPath("$.updatedAt").exists()); // createdAt, updatedAt 필드는 JPA가 자동으로 설정하므로, 테스트에서는 검증하지 않음
	}

	@Test
	@DisplayName("에러 응답 검증 - 상세 메시지 포함")
	void verifyErrorResponse() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(999L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		given(stockCtrlService.create(any(StockDTO.CreateReq.class)))
				.willThrow(new WareException.NotFoundEx("물품 ID 999를 찾을 수 없습니다."));

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("물품 ID 999를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("잘못된 HTTP 메서드 - 405 에러")
	void wrongHttpMethod() throws Exception {
		// When & Then
		mockMvc.perform(patch("/stocks")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	@DisplayName("잘못된 Content-Type - 415 에러")
	void wrongContentType() throws Exception {
		// Given
		StockDTO.CreateReq request = StockDTO.CreateReq.builder()
				.wareId(1L)
				.warehouseId(2L)
				.quantity(100)
				.build();

		// When & Then
		mockMvc.perform(post("/stocks")
						.contentType(MediaType.TEXT_PLAIN)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnsupportedMediaType());
	}

	// ========== 헬퍼 메서드 ==========

	/**
	 * 테스트용 Stock 객체 생성 (리플렉션으로 BaseEntity 필드 설정)
	 */
	private Stock createStockWithId(Long wareId, Long warehouseId, Integer quantity, Long id, Long version) {
		StockKey key = StockKey.of(wareId, warehouseId);
		Stock stock = Stock.builder()
				.key(key)
				.quantity(quantity)
				.build();

		// 리플렉션으로 BaseEntity의 필드들 설정
		setFieldRecursively(stock, "id", id);
		setFieldRecursively(stock, "version", version);

		return stock;
	}

	/**
	 * 리플렉션으로 필드 설정 (부모 클래스 포함)
	 */
	private void setFieldRecursively(Object target, String fieldName, Object value) {
		Class<?> currentClass = target.getClass();

		while (currentClass != null) {
			try {
				Field field = currentClass.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(target, value);
				return; // 성공하면 종료
			} catch (NoSuchFieldException e) {
				// 현재 클래스에 없으면 부모 클래스에서 찾기
				currentClass = currentClass.getSuperclass();
			} catch (Exception e) {
				throw new RuntimeException("Failed to set field: " + fieldName, e);
			}
		}

		throw new RuntimeException("Field not found: " + fieldName);
	}
}