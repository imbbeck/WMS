package com.wms.stock.domain.event;

import com.wms.stock.domain.model.Stock;
import lombok.Getter;

@Getter
public class StockUpdatedEvent extends StockEvent {
	private final Integer oldQuantity;
	private final Integer newQuantity;

	public StockUpdatedEvent(Stock stock, Integer oldQuantity) {
		super(stock.getWareId(), stock.getWarehouseId());
		this.oldQuantity = oldQuantity;
		this.newQuantity = stock.getQuantity();
	}
}
