package com.wms.logisticTask.domain.model;

import lombok.Getter;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 물류 작업 시뮬레이션 이벤트
 * 시간순 이벤트 기반 시뮬레이션에서 사용되는 핵심 도메인 객체
 */
@Getter
public class SimulationEvent implements Comparable<SimulationEvent> {

	private final LogisticTask task;
	private final EventType type;
	private final LocalTime time;
	private final long taskId; // 성능 최적화를 위한 캐싱

	public SimulationEvent(LogisticTask task, EventType type, LocalTime time) {
		this.task = Objects.requireNonNull(task, "작업은 필수입니다");
		this.type = Objects.requireNonNull(type, "이벤트 타입은 필수입니다");
		this.time = Objects.requireNonNull(time, "시간은 필수입니다");
		// ID가 null인 경우를 처리 (새로 생성된 엔티티)
		this.taskId = task.getId() != null ? task.getId() : task.hashCode();
	}

	/**
	 * 시뮬레이션 이벤트 우선순위 비교
	 * 1순위: 시간 (빠른 시간 우선)
	 * 2순위: 이벤트 타입 (COMPLETE → START 순)
	 * 3순위: 작업 ID (낮은 ID 우선)
	 */
	@Override
	public int compareTo(SimulationEvent other) {
		// 1차: 시간순 정렬
		int timeComparison = this.time.compareTo(other.time);
		if (timeComparison != 0) {
			return timeComparison;
		}

		// 2차: 동일 시간일 때 COMPLETE 이벤트 우선 (START=0, COMPLETE=1)
		int typeComparison = Integer.compare(
				other.type.getPriority(),
				this.type.getPriority()
		);
		if (typeComparison != 0) {
			return typeComparison;
		}

		// 3차: 동일 시간, 동일 타입일 때 작업 ID로 일관성 보장 (null 처리)
		return Long.compare(
			this.task.getId() != null ? this.task.getId() : this.task.hashCode(),
			other.task.getId() != null ? other.task.getId() : other.task.hashCode()
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SimulationEvent that = (SimulationEvent) o;
		
		// ID가 있는 경우 ID로 비교, 없는 경우 객체 참조로 비교
		boolean taskEquals;
		if (this.task.getId() != null && that.task.getId() != null) {
			taskEquals = this.task.getId().equals(that.task.getId());
		} else {
			taskEquals = this.task == that.task;
		}
		
		return taskEquals &&
				type == that.type &&
				Objects.equals(time, that.time);
	}

	@Override
	public int hashCode() {
		// ID가 있으면 ID 사용, 없으면 객체 hashCode 사용
		long id = this.task.getId() != null ? this.task.getId() : this.task.hashCode();
		return Objects.hash(id, type, time);
	}

	@Override
	public String toString() {
		return String.format("SimulationEvent{task='%s', type=%s, time=%s}",
				task.getName(), type, time);
	}

	/**
	 * 시뮬레이션 이벤트가 작업 시작인지 확인
	 */
	public boolean isStartEvent() {
		return type == EventType.START;
	}

	/**
	 * 시뮬레이션 이벤트가 작업 완료인지 확인
	 */
	public boolean isCompleteEvent() {
		return type == EventType.COMPLETE;
	}

	/**
	 * 특정 시간 이전의 이벤트인지 확인
	 */
	public boolean isBefore(LocalTime targetTime) {
		return time.isBefore(targetTime);
	}

	/**
	 * 특정 시간 이후의 이벤트인지 확인
	 */
	public boolean isAfter(LocalTime targetTime) {
		return time.isAfter(targetTime);
	}

	/**
	 * 동일한 작업의 이벤트인지 확인
	 */
	public boolean isSameTask(LogisticTask otherTask) {
		// ID가 모두 있는 경우 ID로 비교
		if (this.task.getId() != null && otherTask.getId() != null) {
			return this.task.getId().equals(otherTask.getId());
		}
		// ID가 없는 경우 객체 참조로 비교
		return this.task == otherTask;
	}

	/**
	 * 이벤트 생성 팩토리 메서드들
	 */
	public static SimulationEvent createStartEvent(LogisticTask task) {
		return new SimulationEvent(task, EventType.START, task.getEtd());
	}

	public static SimulationEvent createCompleteEvent(LogisticTask task) {
		return new SimulationEvent(task, EventType.COMPLETE, task.getEta());
	}

	public static SimulationEvent createCustomEvent(LogisticTask task, EventType type, LocalTime time) {
		return new SimulationEvent(task, type, time);
	}
}
