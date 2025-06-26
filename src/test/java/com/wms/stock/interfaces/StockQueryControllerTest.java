package com.wms.stock.interfaces;

import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.location.domain.exception.LocationException;
import com.wms.stock.application.StockQueryService;
import com.wms.stock.domain.exception.StockException;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.dto.StockQueryDTO;
import com.wms.ware.domain.exception.WareException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockQueryController.class)
@Import(TestSecurityConfig.class)
@DisplayName("StockQueryController 테스트")
class StockQueryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private StockQueryService stockQueryService;

	// ========== 기본 재고 조회 테스트 ==========

	@Test
	@DisplayName("GET /stock-queries/by-warehouse-ware - 창고-물품별 재고 조회 성공")
	void getStockByWarehouseAndWare_Success() throws Exception {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;

		StockQueryDTO.Res stockRes = StockQueryDTO.Res.builder()
				.wareId(wareId)
				.wareName("전자제품A")
				.warehouseId(warehouseId)
				.warehouseName("중앙창고")
				.quantity(100)
				.build();

		given(stockQueryService.getStockResByWarehouseAndWare(warehouseId, wareId))
				.willReturn(stockRes);

		// When & Then
		mockMvc.perform(get("/stock-queries/by-warehouse-ware")
						.param("warehouseId", warehouseId.toString())
						.param("wareId", wareId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wareId").value(wareId))
				.andExpect(jsonPath("$.wareName").value("전자제품A"))
				.andExpect(jsonPath("$.warehouseId").value(warehouseId))
				.andExpect(jsonPath("$.warehouseName").value("중앙창고"))
				.andExpect(jsonPath("$.quantity").value(100));
	}

	@Test
	@DisplayName("GET /stock-queries/by-warehouse-ware - 재고 없음")
	void getStockByWarehouseAndWare_NotFound() throws Exception {
		// Given
		Long warehouseId = 1L;
		Long wareId = 999L;

		given(stockQueryService.getStockResByWarehouseAndWare(warehouseId, wareId))
				.willThrow(StockException.notFound(StockKey.of(wareId, warehouseId)));

		// When & Then
		mockMvc.perform(get("/stock-queries/by-warehouse-ware")
						.param("warehouseId", warehouseId.toString())
						.param("wareId", wareId.toString()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /stock-queries/quantity - 재고 수량 조회")
	void getStockQuantity_Success() throws Exception {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;
		Integer quantity = 150;

		given(stockQueryService.getStockQuantity(warehouseId, wareId))
				.willReturn(quantity);

		// When & Then
		mockMvc.perform(get("/stock-queries/quantity")
						.param("warehouseId", warehouseId.toString())
						.param("wareId", wareId.toString()))
				.andExpect(status().isOk())
				.andExpect(content().string(quantity.toString()));
	}

	@Test
	@DisplayName("GET /stock-queries/by-warehouse/{warehouseId} - 창고별 재고 조회")
	void getStocksByWarehouse_Success() throws Exception {
		// Given
		Long warehouseId = 1L;

		List<StockQueryDTO.Res> stockList = Arrays.asList(
				StockQueryDTO.Res.builder()
						.wareId(10L).wareName("물품A")
						.warehouseId(warehouseId).warehouseName("중앙창고")
						.quantity(100).build(),
				StockQueryDTO.Res.builder()
						.wareId(20L).wareName("물품B")
						.warehouseId(warehouseId).warehouseName("중앙창고")
						.quantity(50).build()
		);

		given(stockQueryService.getStocksByWarehouse(warehouseId))
				.willReturn(stockList);

		// When & Then
		mockMvc.perform(get("/stock-queries/by-warehouse/{warehouseId}", warehouseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].wareId").value(10L))
				.andExpect(jsonPath("$[0].wareName").value("물품A"))
				.andExpect(jsonPath("$[0].quantity").value(100))
				.andExpect(jsonPath("$[1].wareId").value(20L))
				.andExpect(jsonPath("$[1].wareName").value("물품B"))
				.andExpect(jsonPath("$[1].quantity").value(50));
	}

	@Test
	@DisplayName("GET /stock-queries/by-warehouse/{warehouseId} - 존재하지 않는 창고")
	void getStocksByWarehouse_WarehouseNotFound() throws Exception {
		// Given
		Long nonExistentWarehouseId = 999L;

		given(stockQueryService.getStocksByWarehouse(nonExistentWarehouseId))
				.willThrow(LocationException.notFound(nonExistentWarehouseId));

		// When & Then
		mockMvc.perform(get("/stock-queries/by-warehouse/{warehouseId}", nonExistentWarehouseId))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /stock-queries/by-ware/{wareId} - 물품별 재고 조회")
	void getStocksByWare_Success() throws Exception {
		// Given
		Long wareId = 1L;

		List<StockQueryDTO.Res> stockList = Arrays.asList(
				StockQueryDTO.Res.builder()
						.wareId(wareId).wareName("전자제품A")
						.warehouseId(10L).warehouseName("창고1")
						.quantity(100).build(),
				StockQueryDTO.Res.builder()
						.wareId(wareId).wareName("전자제품A")
						.warehouseId(20L).warehouseName("창고2")
						.quantity(30).build()
		);

		given(stockQueryService.getStocksByWare(wareId))
				.willReturn(stockList);

		// When & Then
		mockMvc.perform(get("/stock-queries/by-ware/{wareId}", wareId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].warehouseId").value(10L))
				.andExpect(jsonPath("$[0].warehouseName").value("창고1"))
				.andExpect(jsonPath("$[0].quantity").value(100))
				.andExpect(jsonPath("$[1].warehouseId").value(20L))
				.andExpect(jsonPath("$[1].warehouseName").value("창고2"))
				.andExpect(jsonPath("$[1].quantity").value(30));
	}

	// ========== 집계 조회 테스트 ==========

	@Test
	@DisplayName("GET /stock-queries/aggregations/warehouse/{warehouseId} - 창고 집계 조회")
	void getWarehouseAggregation_Success() throws Exception {
		// Given
		Long warehouseId = 1L;

		StockQueryDTO.WarehouseAggregationRes aggregation = StockQueryDTO.WarehouseAggregationRes.builder()
				.warehouseId(warehouseId)
				.warehouseName("중앙창고")
				.totalQuantity(200)
				.capacity(1000)
				.wareTypeCount(3)
				.stockList(Arrays.asList(
						StockQueryDTO.WareUnit.builder()
								.wareId(10L).wareName("물품A").quantity(100).build(),
						StockQueryDTO.WareUnit.builder()
								.wareId(20L).wareName("물품B").quantity(50).build(),
						StockQueryDTO.WareUnit.builder()
								.wareId(30L).wareName("물품C").quantity(50).build()
				))
				.build();

		given(stockQueryService.getWarehouseAggregation(warehouseId))
				.willReturn(aggregation);

		// When & Then
		mockMvc.perform(get("/stock-queries/aggregations/warehouse/{warehouseId}", warehouseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.warehouseId").value(warehouseId))
				.andExpect(jsonPath("$.warehouseName").value("중앙창고"))
				.andExpect(jsonPath("$.totalQuantity").value(200))
				.andExpect(jsonPath("$.capacity").value(1000))
				.andExpect(jsonPath("$.wareTypeCount").value(3))
				.andExpect(jsonPath("$.stockList", hasSize(3)))
				.andExpect(jsonPath("$.stockList[0].wareId").value(10L))
				.andExpect(jsonPath("$.stockList[0].wareName").value("물품A"))
				.andExpect(jsonPath("$.stockList[0].quantity").value(100));
	}

	@Test
	@DisplayName("GET /stock-queries/aggregations/ware/{wareId} - 물품 집계 조회")
	void getWareAggregation_Success() throws Exception {
		// Given
		Long wareId = 1L;

		StockQueryDTO.WareAggregationRes aggregation = StockQueryDTO.WareAggregationRes.builder()
				.wareId(wareId)
				.wareName("전자제품A")
				.totalQuantity(180)
				.warehouseCount(3)
				.stockList(Arrays.asList(
						StockQueryDTO.WarehouseUnit.builder()
								.warehouseId(10L).warehouseName("창고1").quantity(100).build(),
						StockQueryDTO.WarehouseUnit.builder()
								.warehouseId(20L).warehouseName("창고2").quantity(50).build(),
						StockQueryDTO.WarehouseUnit.builder()
								.warehouseId(30L).warehouseName("창고3").quantity(30).build()
				))
				.build();

		given(stockQueryService.getWareAggregation(wareId))
				.willReturn(aggregation);

		// When & Then
		mockMvc.perform(get("/stock-queries/aggregations/ware/{wareId}", wareId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wareId").value(wareId))
				.andExpect(jsonPath("$.wareName").value("전자제품A"))
				.andExpect(jsonPath("$.totalQuantity").value(180))
				.andExpect(jsonPath("$.warehouseCount").value(3))
				.andExpect(jsonPath("$.stockList", hasSize(3)))
				.andExpect(jsonPath("$.stockList[0].warehouseId").value(10L))
				.andExpect(jsonPath("$.stockList[0].warehouseName").value("창고1"))
				.andExpect(jsonPath("$.stockList[0].quantity").value(100));
	}

	@Test
	@DisplayName("GET /stock-queries/aggregations/warehouses - 전체 창고 집계 조회")
	void getAllWarehouseAggregations_Success() throws Exception {
		// Given
		List<StockQueryDTO.WarehouseAggregationRes> aggregations = Arrays.asList(
				StockQueryDTO.WarehouseAggregationRes.builder()
						.warehouseId(1L).warehouseName("중앙창고")
						.totalQuantity(200).capacity(1000).wareTypeCount(2)
						.stockList(Collections.emptyList()).build(),
				StockQueryDTO.WarehouseAggregationRes.builder()
						.warehouseId(2L).warehouseName("지방창고")
						.totalQuantity(150).capacity(500).wareTypeCount(3)
						.stockList(Collections.emptyList()).build()
		);

		given(stockQueryService.getAllWarehouseAggregations())
				.willReturn(aggregations);

		// When & Then
		mockMvc.perform(get("/stock-queries/aggregations/warehouses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].warehouseId").value(1L))
				.andExpect(jsonPath("$[0].warehouseName").value("중앙창고"))
				.andExpect(jsonPath("$[0].totalQuantity").value(200))
				.andExpect(jsonPath("$[1].warehouseId").value(2L))
				.andExpect(jsonPath("$[1].warehouseName").value("지방창고"))
				.andExpect(jsonPath("$[1].totalQuantity").value(150));
	}

	@Test
	@DisplayName("GET /stock-queries/aggregations/wares - 전체 물품 집계 조회")
	void getAllWareAggregations_Success() throws Exception {
		// Given
		List<StockQueryDTO.WareAggregationRes> aggregations = Arrays.asList(
				StockQueryDTO.WareAggregationRes.builder()
						.wareId(1L).wareName("전자제품A")
						.totalQuantity(300).warehouseCount(2)
						.stockList(Collections.emptyList()).build(),
				StockQueryDTO.WareAggregationRes.builder()
						.wareId(2L).wareName("가구B")
						.totalQuantity(150).warehouseCount(1)
						.stockList(Collections.emptyList()).build()
		);

		given(stockQueryService.getAllWareAggregations())
				.willReturn(aggregations);

		// When & Then
		mockMvc.perform(get("/stock-queries/aggregations/wares"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].wareId").value(1L))
				.andExpect(jsonPath("$[0].wareName").value("전자제품A"))
				.andExpect(jsonPath("$[0].totalQuantity").value(300))
				.andExpect(jsonPath("$[1].wareId").value(2L))
				.andExpect(jsonPath("$[1].wareName").value("가구B"))
				.andExpect(jsonPath("$[1].totalQuantity").value(150));
	}

	// ========== 유틸리티 조회 테스트 ==========

	@Test
	@DisplayName("GET /stock-queries/warehouse/{warehouseId}/total-quantity - 창고 총 재고량 조회")
	void getWarehouseTotalQuantity_Success() throws Exception {
		// Given
		Long warehouseId = 1L;
		Integer totalQuantity = 500;

		given(stockQueryService.getWarehouseTotalQuantity(warehouseId))
				.willReturn(totalQuantity);

		// When & Then
		mockMvc.perform(get("/stock-queries/warehouse/{warehouseId}/total-quantity", warehouseId))
				.andExpect(status().isOk())
				.andExpect(content().string(totalQuantity.toString()));
	}

	@Test
	@DisplayName("GET /stock-queries/ware/{wareId}/total-quantity - 물품 총 재고량 조회")
	void getWareTotalQuantity_Success() throws Exception {
		// Given
		Long wareId = 1L;
		Integer totalQuantity = 350;

		given(stockQueryService.getWareTotalQuantity(wareId))
				.willReturn(totalQuantity);

		// When & Then
		mockMvc.perform(get("/stock-queries/ware/{wareId}/total-quantity", wareId))
				.andExpect(status().isOk())
				.andExpect(content().string(totalQuantity.toString()));
	}

	// ========== 예외 처리 테스트 ==========

	@Test
	@DisplayName("존재하지 않는 물품 조회 시 404 에러")
	void getStocksByWare_WareNotFound() throws Exception {
		// Given
		Long nonExistentWareId = 999L;

		given(stockQueryService.getStocksByWare(nonExistentWareId))
				.willThrow(WareException.notFound(nonExistentWareId));

		// When & Then
		mockMvc.perform(get("/stock-queries/by-ware/{wareId}", nonExistentWareId))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("물품 집계 조회 - 존재하지 않는 물품")
	void getWareAggregation_WareNotFound() throws Exception {
		// Given
		Long nonExistentWareId = 999L;

		given(stockQueryService.getWareAggregation(nonExistentWareId))
				.willThrow(WareException.notFound(nonExistentWareId));

		// When & Then
		mockMvc.perform(get("/stock-queries/aggregations/ware/{wareId}", nonExistentWareId))
				.andExpect(status().isNotFound());
	}

	// ========== 파라미터 검증 테스트 ==========

	@Test
	@DisplayName("필수 파라미터 누락 시 400 에러")
	void getStockByWarehouseAndWare_MissingParams() throws Exception {
		// When & Then - wareId 파라미터 누락
		mockMvc.perform(get("/stock-queries/by-warehouse-ware")
						.param("warehouseId", "1"))
				.andExpect(status().isBadRequest());

		// When & Then - warehouseId 파라미터 누락
		mockMvc.perform(get("/stock-queries/by-warehouse-ware")
						.param("wareId", "1"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("잘못된 파라미터 타입 시 400 에러")
	void getStockByWarehouseAndWare_InvalidParamType() throws Exception {
		// When & Then
		mockMvc.perform(get("/stock-queries/by-warehouse-ware")
						.param("warehouseId", "invalid")
						.param("wareId", "1"))
				.andExpect(status().isBadRequest());
	}

	// ========== 빈 결과 처리 테스트 ==========

	@Test
	@DisplayName("빈 재고 목록 반환")
	void getStocksByWarehouse_EmptyResult() throws Exception {
		// Given
		Long warehouseId = 1L;

		given(stockQueryService.getStocksByWarehouse(warehouseId))
				.willReturn(Collections.emptyList());

		// When & Then
		mockMvc.perform(get("/stock-queries/by-warehouse/{warehouseId}", warehouseId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)))
				.andExpect(content().json("[]"));
	}

	@Test
	@DisplayName("0 수량 반환")
	void getStockQuantity_ZeroQuantity() throws Exception {
		// Given
		Long warehouseId = 1L;
		Long wareId = 2L;

		given(stockQueryService.getStockQuantity(warehouseId, wareId))
				.willReturn(0);

		// When & Then
		mockMvc.perform(get("/stock-queries/quantity")
						.param("warehouseId", warehouseId.toString())
						.param("wareId", wareId.toString()))
				.andExpect(status().isOk())
				.andExpect(content().string("0"));
	}
}