package com.wms.logisticTask.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogisticTaskCompletedEvent {
	private Long taskId;
	private Long wareId;
	private Long toLocationId;    // 도착지에만 재고 증가
	private Integer quantity;
}
