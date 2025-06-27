package com.wms.logisticTask.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogisticTaskInitiatedEvent {
	private Long taskId;
	private Long wareId;
	private Long fromLocationId;  // 출발지에서만 재고 감소
	private Integer quantity;
}
