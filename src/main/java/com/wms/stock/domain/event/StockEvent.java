package com.wms.stock.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class StockEvent {
	private final Long wareId;
	private final Long warehouseId;
}