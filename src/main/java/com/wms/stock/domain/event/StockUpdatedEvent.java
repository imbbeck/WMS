package com.wms.stock.domain.event;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import lombok.Getter;

@Getter
public class StockUpdatedEvent {
	private final StockKey key;
	private final Integer oldQuantity;
	private final Integer newQuantity;

	public StockUpdatedEvent(Stock stock, Integer oldQuantity) {
		this.key = stock.getKey();
		this.oldQuantity = oldQuantity;
		this.newQuantity = stock.getQuantity();
	}
}
