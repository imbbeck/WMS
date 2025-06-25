package com.wms.logisticTask.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * 시뮬레이션 실행 통계 정보
 */
@Getter
@Builder
public class SimulationStats {

	private final int totalEvents;
	private final int totalTasks;
	private final LocalTime startTime;
	private final LocalTime endTime;
	private final long processingTimeMs;
	private final Map<EventType, Integer> eventTypeCounts;
	private final boolean successful;
	private final String failureReason;

	/**
	 * 성공적인 시뮬레이션 통계 생성
	 */
	public static SimulationStats success(int totalEvents, int totalTasks,
			LocalTime startTime, LocalTime endTime,
			long processingTimeMs,
			Map<EventType, Integer> eventTypeCounts) {
		return SimulationStats.builder()
				.totalEvents(totalEvents)
				.totalTasks(totalTasks)
				.startTime(startTime)
				.endTime(endTime)
				.processingTimeMs(processingTimeMs)
				.eventTypeCounts(new EnumMap<>(eventTypeCounts))
				.successful(true)
				.build();
	}

	/**
	 * 실패한 시뮬레이션 통계 생성
	 */
	public static SimulationStats failure(String reason, long processingTimeMs) {
		return SimulationStats.builder()
				.successful(false)
				.failureReason(reason)
				.processingTimeMs(processingTimeMs)
				.eventTypeCounts(new EnumMap<>(EventType.class))
				.build();
	}

	/**
	 * 시뮬레이션 기간 계산 (분 단위)
	 */
	public long getSimulationDurationMinutes() {
		if (startTime == null || endTime == null) {
			return 0;
		}
		return java.time.Duration.between(startTime, endTime).toMinutes();
	}

	/**
	 * 초당 처리된 이벤트 수
	 */
	public double getEventsPerSecond() {
		if (processingTimeMs <= 0) {
			return 0;
		}
		return (double) totalEvents / (processingTimeMs / 1000.0);
	}

	/**
	 * 특정 이벤트 타입의 처리 개수
	 */
	public int getEventCount(EventType eventType) {
		return eventTypeCounts.getOrDefault(eventType, 0);
	}

	/**
	 * 시뮬레이션 성능이 양호한지 확인
	 */
	public boolean isPerformanceGood() {
		return successful && processingTimeMs < 1000; // 1초 미만
	}

	/**
	 * 시뮬레이션 성능 등급 반환
	 */
	public PerformanceGrade getPerformanceGrade() {
		if (!successful) {
			return PerformanceGrade.FAILED;
		}

		if (processingTimeMs < 100) {
			return PerformanceGrade.EXCELLENT;
		} else if (processingTimeMs < 500) {
			return PerformanceGrade.GOOD;
		} else if (processingTimeMs < 1000) {
			return PerformanceGrade.FAIR;
		} else {
			return PerformanceGrade.POOR;
		}
	}

	@Override
	public String toString() {
		if (!successful) {
			return String.format("SimulationStats{FAILED: %s, duration=%dms}",
					failureReason, processingTimeMs);
		}

		return String.format(
				"SimulationStats{events=%d, tasks=%d, timeRange=%s~%s, duration=%dms, " +
						"performance=%s, eventBreakdown=%s}",
				totalEvents, totalTasks, startTime, endTime, processingTimeMs,
				getPerformanceGrade(), eventTypeCounts
		);
	}

	/**
	 * 성능 등급
	 */
	@Getter
	public enum PerformanceGrade {
		EXCELLENT("우수"),
		GOOD("양호"),
		FAIR("보통"),
		POOR("느림"),
		FAILED("실패");

		private final String description;

		PerformanceGrade(String description) {
			this.description = description;
		}

	}
}