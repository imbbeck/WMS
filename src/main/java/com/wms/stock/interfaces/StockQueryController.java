package com.wms.stock.interfaces;

import java.util.List;
import java.util.Optional;

import com.wms.stock.application.StockQueryService;
import com.wms.stock.domain.exception.StockException;
import com.wms.stock.domain.model.StockKey;
import com.wms.stock.dto.StockQueryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-queries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "재고 조회", description = "최적화된 읽기 작업으로 재고 정보 조회 API")
public class StockQueryController {

	private final StockQueryService stockQueryService;

	// === 기본 재고 조회 ===

	@GetMapping("/by-warehouse-ware")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "창고-물품별 재고 조회",
			description = "특정 창고-물품 조합의 재고 정보를 조회합니다. " +
					"최적화된 성능을 위해 캐시를 사용합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "재고 정보 조회 성공"),
			@ApiResponse(responseCode = "404", description = "해당 조합의 재고를 찾을 수 없음")
	})
	public StockQueryDTO.Res getStockByWarehouseAndWare(
			@Parameter(description = "창고 ID") @RequestParam Long warehouseId,
			@Parameter(description = "물품 ID") @RequestParam Long wareId
	) {
		log.debug("창고-물품별 재고 조회 - warehouseId: {}, wareId: {}", warehouseId, wareId);
		return stockQueryService.getStockResByWarehouseAndWare(warehouseId, wareId);
	}

	@GetMapping("/quantity")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "재고 수량 조회",
			description = "특정 창고-물품 조합의 수량만 조회합니다. " +
					"빠른 수량 확인을 위해 최적화되었습니다.")
	@ApiResponse(responseCode = "200", description = "재고 수량 조회 성공")
	public Integer getStockQuantity(
			@Parameter(description = "창고 ID") @RequestParam Long warehouseId,
			@Parameter(description = "물품 ID") @RequestParam Long wareId) {
		log.debug("재고 수량 조회 - warehouseId: {}, wareId: {}", warehouseId, wareId);

		return stockQueryService.getStockQuantity(warehouseId, wareId);
	}

	@GetMapping("/by-warehouse/{warehouseId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "창고 내 전체 재고 조회",
			description = "특정 창고의 모든 재고 항목을 조회합니다. " +
					"최적화된 성능을 위해 캐시를 사용합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "창고 재고 조회 성공"),
			@ApiResponse(responseCode = "404", description = "창고를 찾을 수 없음")
	})
	public List<StockQueryDTO.Res> getStocksByWarehouse(
			@Parameter(description = "창고 ID") @PathVariable Long warehouseId) {
		log.debug("창고별 재고 조회 - warehouseId: {}", warehouseId);

		return stockQueryService.getStocksByWarehouse(warehouseId);
	}

	@GetMapping("/by-ware/{wareId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "물품별 재고 조회",
			description = "모든 창고에서 특정 물품의 재고 항목을 조회합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "물품별 재고 조회 성공"),
			@ApiResponse(responseCode = "404", description = "물품을 찾을 수 없음")
	})
	public List<StockQueryDTO.Res> getStocksByWare(
			@Parameter(description = "물품 ID") @PathVariable Long wareId) {
		log.debug("물품별 재고 조회 - wareId: {}", wareId);

		return stockQueryService.getStocksByWare(wareId);
	}

	// === 집계 조회 ===

	@GetMapping("/aggregations/warehouse/{warehouseId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "창고 재고 집계 조회",
			description = "특정 창고의 집계된 재고 정보를 조회합니다. " +
					"총 수량, 용량 활용률, 물품 타입별 분석을 포함합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "창고 집계 조회 성공"),
			@ApiResponse(responseCode = "404", description = "창고를 찾을 수 없음")
	})
	public StockQueryDTO.WarehouseAggregationRes getWarehouseAggregation(
			@Parameter(description = "창고 ID") @PathVariable Long warehouseId) {
		log.debug("창고 집계 조회 - warehouseId: {}", warehouseId);

		return stockQueryService.getWarehouseAggregation(warehouseId);
	}

	@GetMapping("/aggregations/ware/{wareId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "물품 재고 집계 조회",
			description = "특정 물품의 집계된 재고 정보를 조회합니다. " +
					"모든 창고의 총 수량과 창고별 분포를 포함합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "물품 집계 조회 성공"),
			@ApiResponse(responseCode = "404", description = "물품을 찾을 수 없음")
	})
	public StockQueryDTO.WareAggregationRes getWareAggregation(
			@Parameter(description = "물품 ID") @PathVariable Long wareId) {
		log.debug("물품 집계 조회 - wareId: {}", wareId);

		return stockQueryService.getWareAggregation(wareId);
	}

	@GetMapping("/aggregations/warehouses")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "전체 창고 집계 조회",
			description = "모든 창고의 집계된 재고 정보를 조회합니다. " +
					"대시보드 화면과 용량 계획에 유용합니다.")
	@ApiResponse(responseCode = "200", description = "전체 창고 집계 조회 성공")
	public List<StockQueryDTO.WarehouseAggregationRes> getAllWarehouseAggregations() {
		log.debug("전체 창고 집계 조회");

		return stockQueryService.getAllWarehouseAggregations();
	}

	@GetMapping("/aggregations/wares")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "전체 물품 집계 조회",
			description = "모든 물품의 집계된 재고 정보를 조회합니다. " +
					"재고 분석과 조달 계획에 유용합니다.")
	@ApiResponse(responseCode = "200", description = "전체 물품 집계 조회 성공")
	public List<StockQueryDTO.WareAggregationRes> getAllWareAggregations() {
		log.debug("전체 물품 집계 조회");

		return stockQueryService.getAllWareAggregations();
	}

	// === 유틸리티 조회 ===

	@GetMapping("/warehouse/{warehouseId}/total-quantity")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "창고 총 재고 수량 조회",
			description = "특정 창고 내 모든 재고의 총 수량을 조회합니다. " +
					"빠른 용량 확인을 위해 최적화되었습니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "총 수량 조회 성공"),
			@ApiResponse(responseCode = "404", description = "창고를 찾을 수 없음")
	})
	public Integer getWarehouseTotalQuantity(
			@Parameter(description = "창고 ID") @PathVariable Long warehouseId) {
		log.debug("창고 총 재고량 조회 - warehouseId: {}", warehouseId);

		return stockQueryService.getWarehouseTotalQuantity(warehouseId);
	}

	@GetMapping("/ware/{wareId}/total-quantity")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "물품 총 재고 수량 조회",
			description = "모든 창고에서 특정 물품의 총 수량을 조회합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "총 수량 조회 성공"),
			@ApiResponse(responseCode = "404", description = "물품을 찾을 수 없음")
	})
	public Integer getWareTotalQuantity(
			@Parameter(description = "물품 ID") @PathVariable Long wareId) {
		log.debug("물품 총 재고량 조회 - wareId: {}", wareId);

		return stockQueryService.getWareTotalQuantity(wareId);
	}

}
