package com.wms.logisticTask.domain.model;

public enum LogisticTaskStatus {
	PENDING,    // 대기중
	INITIATED,  // 작업시작
	INITIATE_DELAYED,   // 작업시작 딜레이
	COMPLETED,  // 작업완료
	COMPLETE_DELAYED,   // 작업완료 딜레이
	CANCELLED,  // 취소. 취소 상태의 작업은 아직 etd가 현재시간 이전일때만 PENDING상태로 전환가능
	FAILED      // 작업 중 실패. COMPLETED 이벤트 실행x
}
