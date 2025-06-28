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
@Tag(name = "재고 관리 (관리자 전용)", description = "창고 재고 관리 API")
public class StockController {

	private final StockCtrlService stockCtrlService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "새 재고 항목 생성",
			description = "창고의 특정 물품에 대한 새로운 재고 항목을 생성합니다. " +
					"초기 재고 설정이나 관리자의 재고 조정에 사용됩니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "재고 항목 생성 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 비즈니스 규칙 위반"),
			@ApiResponse(responseCode = "409", description = "해당 물품-창고 조합의 재고 항목이 이미 존재")
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
	@Operation(summary = "재고 수량 수정 (비즈니스 키 기준: 창고ID, 물품ID)",
			description = "기존 재고 항목의 수량을 수정합니다. " +
					"일반적인 물류 흐름 제어를 우회하므로 신중하게 사용해야 합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "재고 수정 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 비즈니스 규칙 위반"),
			@ApiResponse(responseCode = "404", description = "재고 항목을 찾을 수 없음"),
			@ApiResponse(responseCode = "409", description = "낙관적 락 충돌")
	})
	public StockDTO.Res updateStock(
			@Parameter(description = "창고 ID") @PathVariable Long warehouseId,
			@Parameter(description = "물품 ID") @PathVariable Long wareId,
			@Valid @RequestBody StockDTO.UpdateReq request) {

		log.info("재고 수정 요청 - warehouseId: {}, wareId: {}, updatedQuantity: {}", warehouseId, wareId, request.getQuantity());

		Stock stock = stockCtrlService.update(wareId, warehouseId, request);
		log.info("재고 수정 완료 - stockId: {}, updatedQuantity: {}", stock.getId(), stock.getQuantity());

		return StockDTO.Res.from(stock);
	}

	@DeleteMapping("/{warehouseId}/{wareId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "재고 항목 삭제 (비즈니스 키 기준: 창고ID, 물품ID)",
			description = "재고 항목을 삭제합니다. 시스템 일관성에 영향을 줄 수 있으므로 신중하게 사용해야 합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "재고 항목 삭제 성공"),
			@ApiResponse(responseCode = "404", description = "재고 항목을 찾을 수 없음"),
			@ApiResponse(responseCode = "409", description = "활성 의존성이 있어 삭제할 수 없음")
	})
	public void deleteStock(@Parameter(description = "창고 ID") @PathVariable Long warehouseId,
	                        @Parameter(description = "물품 ID") @PathVariable Long wareId) {
		log.info("재고 삭제 요청 - warehouseId: {}, wareId: {}", warehouseId, wareId);

		stockCtrlService.delete(wareId, warehouseId);
		log.info("재고 삭제 완료 - warehouseId: {}, wareId: {}", warehouseId, wareId);
	}
}
