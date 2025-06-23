package com.wms.stock.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public abstract class StockEvent {
	private final Long stockId;
	private final Long wareId;
	private final Long warehouseId;
	private final LocalDateTime occurredAt = LocalDateTime.now();
}