package com.wms.stock.domain.event;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class StockChangeEvent {
	private Long taskId;
	private Long wareId;
	private Long locationId;
	private StockChangeType changeType;
	private Integer quantity;
	private LocalDateTime timestamp;
}