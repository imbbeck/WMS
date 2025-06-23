package com.wms.stock.domain.event;

import com.wms.applicationInfra.idnameMapCashing.ReferenceEvent;
import lombok.Getter;

@Getter
public class StockUpdatedEvent extends StockEvent {
	private final Integer oldQuantity;
	private final Integer newQuantity;

	public StockUpdatedEvent(Long stockId, Long wareId, Long warehouseId, Integer oldQuantity, Integer newQuantity) {
		super(stockId, wareId, warehouseId);
		this.oldQuantity = oldQuantity;
		this.newQuantity = newQuantity;
	}
}
