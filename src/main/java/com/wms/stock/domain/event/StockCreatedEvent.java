package com.wms.stock.domain.event;

import lombok.Getter;

@Getter
public class StockCreatedEvent extends StockEvent {
	private final Integer quantity;

	public StockCreatedEvent(Long stockId, Long wareId, Long warehouseId, Integer quantity) {
		super(stockId, wareId, warehouseId);
		this.quantity = quantity;
	}
}
