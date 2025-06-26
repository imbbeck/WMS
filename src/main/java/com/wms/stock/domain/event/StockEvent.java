package com.wms.stock.domain.event;

import com.wms.stock.domain.model.StockKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class StockEvent {
	private final StockKey key;
}