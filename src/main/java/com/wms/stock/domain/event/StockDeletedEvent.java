package com.wms.stock.domain.event;

import com.wms.stock.domain.model.Stock;
import lombok.Getter;

@Getter
public class StockDeletedEvent extends StockEvent {
	private final Integer quantity;

	public StockDeletedEvent(Stock stock) {
		super(stock.getWareId(), stock.getWarehouseId());
		this.quantity = stock.getQuantity();
	}
}
