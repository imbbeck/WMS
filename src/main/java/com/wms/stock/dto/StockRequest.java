package com.wms.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter

public class StockRequest {
    @NotNull(message = "물품 ID는 필수입니다")
    private Long wareId;

    @NotNull(message = "장소 ID는 필수입니다")
    private Long locationId;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 0, message = "수량은 0 이상이어야 합니다")
    private Integer quantity;

	public StockRequest(Long wareId, Long toLocationId, Integer quantity) {
	}
}