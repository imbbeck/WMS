package com.wms.logisticTask.domain.event;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogisticTaskInitiatedEvent {
	private Long taskId;
	private Long wareId;
	private Long fromLocationId;
	private Integer quantity;
	private LocalDateTime initiatedAt;
}
