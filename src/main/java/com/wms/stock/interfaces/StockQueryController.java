package com.wms.stock.interfaces;

import java.util.List;
import java.util.Optional;

import com.wms.stock.application.StockQueryService;
import com.wms.stock.domain.exception.StockException;
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
@Tag(name = "Stock Query", description = "APIs for querying stock information with optimized read operations")
public class StockQueryController {

	private final StockQueryService stockQueryService;

	// === 기본 재고 조회 ===

	@GetMapping("/by-warehouse-ware")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get stock by warehouse and ware",
			description = "Retrieves stock information for a specific warehouse-ware combination. " +
					"Uses cache for optimized performance.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Stock information retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Stock not found for the given combination")
	})
	public Optional<StockQueryDTO.Res> getStockByWarehouseAndWare(
			@Parameter(description = "Warehouse ID") @RequestParam Long warehouseId,
			@Parameter(description = "Ware ID") @RequestParam Long wareId) {
		log.debug("창고-물품별 재고 조회 - warehouseId: {}, wareId: {}", warehouseId, wareId);

		return Optional.ofNullable(stockQueryService.getStockResByWarehouseAndWare(warehouseId, wareId).orElseThrow(() -> StockException.notFound(warehouseId + ", " + wareId)
		));
	}

	@GetMapping("/quantity")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get stock quantity",
			description = "Retrieves only the quantity for a specific warehouse-ware combination. " +
					"Optimized for quick quantity checks.")
	@ApiResponse(responseCode = "200", description = "Stock quantity retrieved successfully")
	public Integer getStockQuantity(
			@Parameter(description = "Warehouse ID") @RequestParam Long warehouseId,
			@Parameter(description = "Ware ID") @RequestParam Long wareId) {
		log.debug("재고 수량 조회 - warehouseId: {}, wareId: {}", warehouseId, wareId);

		return stockQueryService.getStockQuantity(warehouseId, wareId);
	}

	@GetMapping("/by-warehouse/{warehouseId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get all stocks in warehouse",
			description = "Retrieves all stock entries for a specific warehouse. " +
					"Uses cache for optimized performance.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Warehouse stocks retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Warehouse not found")
	})
	public List<StockQueryDTO.Res> getStocksByWarehouse(
			@Parameter(description = "Warehouse ID") @PathVariable Long warehouseId) {
		log.debug("창고별 재고 조회 - warehouseId: {}", warehouseId);

		return stockQueryService.getStocksByWarehouse(warehouseId);
	}

	@GetMapping("/by-ware/{wareId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get stocks by ware",
			description = "Retrieves all stock entries for a specific ware across all warehouses.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ware stocks retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Ware not found")
	})
	public List<StockQueryDTO.Res> getStocksByWare(
			@Parameter(description = "Ware ID") @PathVariable Long wareId) {
		log.debug("물품별 재고 조회 - wareId: {}", wareId);

		return stockQueryService.getStocksByWare(wareId);
	}

	// === 집계 조회 ===

	@GetMapping("/aggregations/warehouse/{warehouseId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get warehouse stock aggregation",
			description = "Retrieves aggregated stock information for a specific warehouse including " +
					"total quantity, capacity utilization, and breakdown by ware types.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Warehouse aggregation retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Warehouse not found")
	})
	public StockQueryDTO.WarehouseAggregationRes getWarehouseAggregation(
			@Parameter(description = "Warehouse ID") @PathVariable Long warehouseId) {
		log.debug("창고 집계 조회 - warehouseId: {}", warehouseId);

		return stockQueryService.getWarehouseAggregation(warehouseId);
	}

	@GetMapping("/aggregations/ware/{wareId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get ware stock aggregation",
			description = "Retrieves aggregated stock information for a specific ware including " +
					"total quantity across all warehouses and warehouse distribution.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ware aggregation retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Ware not found")
	})
	public StockQueryDTO.WareAggregationRes getWareAggregation(
			@Parameter(description = "Ware ID") @PathVariable Long wareId) {
		log.debug("물품 집계 조회 - wareId: {}", wareId);

		return stockQueryService.getWareAggregation(wareId);
	}

	@GetMapping("/aggregations/warehouses")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get all warehouse aggregations",
			description = "Retrieves aggregated stock information for all warehouses. " +
					"Useful for dashboard views and capacity planning.")
	@ApiResponse(responseCode = "200", description = "All warehouse aggregations retrieved successfully")
	public List<StockQueryDTO.WarehouseAggregationRes> getAllWarehouseAggregations() {
		log.debug("전체 창고 집계 조회");

		return stockQueryService.getAllWarehouseAggregations();
	}

	@GetMapping("/aggregations/wares")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get all ware aggregations",
			description = "Retrieves aggregated stock information for all wares. " +
					"Useful for inventory analysis and procurement planning.")
	@ApiResponse(responseCode = "200", description = "All ware aggregations retrieved successfully")
	public List<StockQueryDTO.WareAggregationRes> getAllWareAggregations() {
		log.debug("전체 물품 집계 조회");

		return stockQueryService.getAllWareAggregations();
	}

	// === 유틸리티 조회 ===

	@GetMapping("/warehouse/{warehouseId}/total-quantity")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get warehouse total stock quantity",
			description = "Retrieves the total quantity of all stock in a specific warehouse. " +
					"Optimized for quick capacity checks.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Total quantity retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Warehouse not found")
	})
	public Integer getWarehouseTotalQuantity(
			@Parameter(description = "Warehouse ID") @PathVariable Long warehouseId) {
		log.debug("창고 총 재고량 조회 - warehouseId: {}", warehouseId);

		return stockQueryService.getWarehouseTotalQuantity(warehouseId);
	}

	@GetMapping("/ware/{wareId}/total-quantity")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get ware total stock quantity",
			description = "Retrieves the total quantity of a specific ware across all warehouses.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Total quantity retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Ware not found")
	})
	public Integer getWareTotalQuantity(
			@Parameter(description = "Ware ID") @PathVariable Long wareId) {
		log.debug("물품 총 재고량 조회 - wareId: {}", wareId);

		return stockQueryService.getWareTotalQuantity(wareId);
	}

}