package com.wms.logisticTask.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogisticTaskCompletedEvent {
	private Long taskId;
	private Long wareId;
	private Long toLocationId;
	private Integer quantity;
}
