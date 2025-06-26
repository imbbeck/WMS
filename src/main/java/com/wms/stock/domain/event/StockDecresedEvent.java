package com.wms.stock.domain.event;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import lombok.Getter;

@Getter
public class StockDecresedEvent {
	private final StockKey key;
	private final Integer quantity;

	public StockDecresedEvent(Stock stock) {
		this.key = stock.getKey();
		this.quantity = stock.getQuantity();
	}
}
