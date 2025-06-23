package com.wms.stock.domain.event;

import com.wms.stock.domain.model.Stock;
import lombok.Getter;

@Getter
public class StockIncreasedEvent extends StockEvent {
	private final Integer quantity;

	public StockIncreasedEvent(Stock stock) {
		super( stock.getWareId(), stock.getWarehouseId());
		this.quantity = stock.getQuantity();
	}
}
