package com.wms.logisticTask.domain.model;

import java.util.Map;

public enum LogisticTaskStatus {
	PENDING,    // 대기중
	INITIATED,  // 작업시작
	INITIATE_DELAYED,   // 작업시작 딜레이
	COMPLETED,  // 작업완료
	COMPLETE_DELAYED,   // 작업완료 딜레이
	CANCELLED,  // 취소. 취소 상태의 작업은 아직 etd가 현재시간 이전일때만 PENDING상태로 전환가능
	FAILED,      // 작업 중 실패. COMPLETED 이벤트 실행x
	EXPIRED;     // 만료. 배치 작업시 PENDING, INITIATE_DELAYED 상태의 작업을 EXPIRED로 전환

	private static final Map<LogisticTaskStatus, String> COLOR_MAP = Map.of(
			PENDING, "#EF4444",
			INITIATED, "#F97316",
			INITIATE_DELAYED, " #FACC15",
			COMPLETED, "#22C55E",
			COMPLETE_DELAYED, "#3B82F6",
			CANCELLED, "#1E3A8A",
			FAILED, "#8B5CF6",
			EXPIRED, "#6B7280"
	);

	public String getColor() {
		return COLOR_MAP.get(this);
	}
}
