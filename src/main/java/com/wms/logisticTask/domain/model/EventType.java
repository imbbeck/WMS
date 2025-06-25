package com.wms.logisticTask.domain.model;

import lombok.Getter;

/**
 * 시뮬레이션 이벤트 타입
 */
@Getter
public enum EventType {

	/**
	 * 작업 시작 이벤트 (ETD 시점)
	 * - 출발 창고에서 재고 차감
	 * - 작업자 할당
	 */
	START(0, "작업 시작"),

	/**
	 * 작업 완료 이벤트 (ETA 시점)
	 * - 도착 창고에 재고 증가
	 * - 창고 용량 검증
	 * - 작업자 해제
	 */
	COMPLETE(1, "작업 완료"),

	/**
	 * 작업 지연 이벤트 (향후 확장용)
	 * - 예상 시간 지연 발생
	 * - 후속 작업들에 영향 전파
	 */
	DELAY(2, "작업 지연"),

	/**
	 * 작업 실패 이벤트 (향후 확장용)
	 * - 작업 중단
	 * - 재고 롤백 처리
	 */
	FAILURE(3, "작업 실패");

	private final int priority;
	private final String description;

	EventType(int priority, String description) {
		this.priority = priority;
		this.description = description;
	}

	/**
	 * 이벤트 타입이 재고에 영향을 주는지 확인
	 */
	public boolean affectsStock() {
		return this == START || this == COMPLETE || this == FAILURE;
	}

	/**
	 * 이벤트 타입이 창고 용량에 영향을 주는지 확인
	 */
	public boolean affectsCapacity() {
		return this == START || this == COMPLETE;
	}

	/**
	 * 이벤트 타입이 작업자 스케줄에 영향을 주는지 확인
	 */
	public boolean affectsWorkerSchedule() {
		return this == START || this == COMPLETE || this == FAILURE;
	}
}