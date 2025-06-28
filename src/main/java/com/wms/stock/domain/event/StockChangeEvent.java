package com.wms.stock.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockChangeEvent {
	private Long taskId;
	private Long wareId;
	private Long locationId;
	private StockChangeType changeType;
	private Integer quantity;
	private LocalDateTime timestamp;
}