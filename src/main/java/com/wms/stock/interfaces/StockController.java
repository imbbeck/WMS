package com.wms.stock.interfaces;

import com.wms.stock.application.StockCtrlService;
import com.wms.stock.domain.model.Stock;
import com.wms.stock.dto.StockDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stock Management for only admin", description = "APIs for managing warehouse stock inventories")
public class StockController {

	private final StockCtrlService stockCtrlService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create new stock entry",
			description = "Creates a new stock entry for a specific ware in a warehouse. " +
					"This is used for initial stock setup or administrative adjustments.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Stock entry created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data or business rule violation"),
			@ApiResponse(responseCode = "409", description = "Stock entry already exists for this ware-warehouse combination")
	})
	public StockDTO.Res createStock(@Valid @RequestBody StockDTO.CreateReq request) {
		log.info("재고 생성 요청 - wareId: {}, warehouseId: {}, quantity: {}",
				request.getWareId(), request.getWarehouseId(), request.getQuantity());

		Stock stock = stockCtrlService.create(request);
		log.info("재고 생성 완료 - stockId: {}", stock.getId());

		return StockDTO.Res.from(stock);
	}

	@PutMapping("/{warehouseId}/{wareId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Update stock quantity By Business Key(warehouseId, wareId)",
			description = "Updates the quantity of an existing stock entry. " +
					"This should be used carefully as it bypasses normal logistic flow controls.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Stock updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data or business rule violation"),
			@ApiResponse(responseCode = "404", description = "Stock entry not found"),
			@ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
	})
	public StockDTO.Res updateStock(
			@Parameter(description = "Warehouse ID") @PathVariable Long warehouseId,
			@Parameter(description = "Ware ID") @PathVariable Long wareId,
			@Valid @RequestBody StockDTO.UpdateReq request) {

		log.info("재고 수정 요청 - warehouseId: {}, wareId: {}, updatedQuantity: {}", warehouseId, wareId, request.getQuantity());

		Stock stock = stockCtrlService.update(wareId, warehouseId, request);
		log.info("재고 수정 완료 - stockId: {}, updatedQuantity: {}", stock.getId(), stock.getQuantity());

		return StockDTO.Res.from(stock);
	}

	@DeleteMapping("/{warehouseId}/{wareId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete stock entry By Business Key(warehouseId, wareId)",
			description = "Deletes a stock entry. This should be used carefully as it may affect system consistency.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Stock entry deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Stock entry not found"),
			@ApiResponse(responseCode = "409", description = "Cannot delete stock with active dependencies")
	})
	public void deleteStock(@Parameter(description = "Warehouse ID") @PathVariable Long warehouseId,
	                        @Parameter(description = "Ware ID") @PathVariable Long wareId) {
		log.info("재고 삭제 요청 - warehouseId: {}, wareId: {}", warehouseId, wareId);

		stockCtrlService.delete(wareId, warehouseId);
		log.info("재고 삭제 완료 - warehouseId: {}, wareId: {}", warehouseId, wareId);
	}
}