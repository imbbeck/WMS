package com.wms.stock.domain.event;

import com.wms.stock.domain.model.Stock;
import lombok.Getter;

@Getter
public class StockDecresedEvent extends StockEvent {
	private final Integer quantity;

	public StockDecresedEvent(Stock stock) {
		super( stock.getWareId(), stock.getWarehouseId());
		this.quantity = stock.getQuantity();
	}
}
