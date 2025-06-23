package com.wms.stock.domain.event;

import lombok.Getter;

@Getter
public class StockDeletedEvent extends StockEvent {
	private final Integer quantity;

	public StockDeletedEvent(Long stockId, Long wareId, Long warehouseId, Integer quantity) {
		super(stockId, wareId, warehouseId);
		this.quantity = quantity;
	}
}
