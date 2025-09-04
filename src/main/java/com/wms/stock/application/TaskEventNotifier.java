package com.wms.stock.application;

import com.wms.notification.application.StockSyncNotificationAdapter;
import com.wms.notification.domain.model.StockSyncStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 작업 이벤트 알림 컴포넌트
 * 
 * 물류 작업의 생명주기와 재고 동기화 과정에서 발생하는
 * 다양한 이벤트를 사용자에게 실시간으로 알립니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventNotifier {

	private final StockSyncNotificationAdapter stockSyncNotificationAdapter;

	// =============  작업 라이프사이클 이벤트 =============

	public void notifyTaskInitiationStarted(Long taskId) {
		broadcastEvent(taskId, StockSyncStatus.PROCESSING, "작업 시작 처리 중...");
	}

	public void notifyTaskInitiated(Long taskId) {
		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업이 시작되었습니다");
	}

	public void notifyTaskInitiatedWithoutStock(Long taskId, String reason) {
		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업 시작 완료 (" + reason + ")");
	}

	public void notifyTaskCompletionStarted(Long taskId) {
		broadcastEvent(taskId, StockSyncStatus.PROCESSING, "작업 완료 처리 중...");
	}

	public void notifyTaskCompleted(Long taskId) {
		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업이 완료되었습니다");
	}

	public void notifyTaskCompletedWithoutStock(Long taskId, String reason) {
		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업 완료 (" + reason + ")");
	}

	// =============  재고 동기화 이벤트 =============

	public void notifyStockSyncQueued(Long taskId, String details) {
		broadcastEvent(taskId, StockSyncStatus.QUEUED, "재고 동기화 대기: " + details);
	}

	public void notifyStockSyncProcessing(Long taskId, String operation) {
		broadcastEvent(taskId, StockSyncStatus.PROCESSING, "재고 " + operation + " 진행 중...");
	}

	public void notifyStockSyncRetrying(Long taskId, int retryCount) {
		broadcastEvent(taskId, StockSyncStatus.RETRYING,
				String.format("동시성 충돌로 재시도 중... (%d회)", retryCount));
	}

	public void notifyStockSyncCompleted(Long taskId) {
		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "재고 동기화가 완료되었습니다");
	}

	// =============  오류 이벤트 =============

	public void notifyValidationFailed(Long taskId, String reason) {
		broadcastEvent(taskId, StockSyncStatus.FAILED, "검증 실패: " + reason);
	}

	public void notifyCapacityExceeded(Long taskId, Long warehouseId, int current, int requested, int capacity) {
		String message = String.format("창고(%d) 용량 초과: 현재 %d + 요청 %d > 한계 %d",
				warehouseId, current, requested, capacity);
		broadcastEvent(taskId, StockSyncStatus.FAILED, message);
	}

	public void notifyInsufficientStock(Long taskId, Long warehouseId, Long wareId, int current, int requested) {
		String message = String.format("재고 부족: 창고(%d) 물품(%d) 현재 %d < 요청 %d",
				warehouseId, wareId, current, requested);
		broadcastEvent(taskId, StockSyncStatus.FAILED, message);
	}

	public void notifyStreamPublishFailed(Long taskId, String streamKey, String error) {
		broadcastEvent(taskId, StockSyncStatus.FAILED,
				String.format("스트림 발행 실패(%s): %s", streamKey, error));
	}

	public void notifyProcessingError(Long taskId, String error) {
		broadcastEvent(taskId, StockSyncStatus.FAILED, "처리 오류: " + error);
	}

	// =============  새로운 편의 메서드들 =============

	/**
	 * 작업 시작 프로세스 전체를 간편하게 알림
	 */
	public void notifyTaskInitiationFlow(Long taskId, boolean hasStockChange, String details) {
		if (hasStockChange) {
			notifyTaskInitiationStarted(taskId);
			// 실제 재고 처리는 별도 컴포넌트에서 수행
			// 완료는 해당 컴포넌트에서 notifyTaskInitiated() 호출
		} else {
			notifyTaskInitiatedWithoutStock(taskId, details);
		}
	}

	/**
	 * 작업 완료 프로세스 전체를 간편하게 알림
	 */
	public void notifyTaskCompletionFlow(Long taskId, boolean hasStockChange, String details) {
		if (hasStockChange) {
			notifyTaskCompletionStarted(taskId);
			// 실제 재고 처리는 별도 컴포넌트에서 수행
			// 완료는 해당 컴포넌트에서 notifyTaskCompleted() 호출
		} else {
			notifyTaskCompletedWithoutStock(taskId, details);
		}
	}

	/**
	 * 재고 동기화 전체 프로세스 상태 추적
	 */
	public void notifyStockSyncFlow(Long taskId, String operation, boolean isRetry, int retryCount) {
		if (isRetry) {
			notifyStockSyncRetrying(taskId, retryCount);
		} else {
			notifyStockSyncProcessing(taskId, operation);
		}
	}

	/**
	 * 상세한 에러 정보와 함께 알림
	 */
	public void notifyDetailedError(Long taskId, String errorType, String details, Exception exception) {
		String message = String.format("%s: %s", errorType, details);
		if (exception != null) {
			message += " (원인: " + exception.getMessage() + ")";
		}
		broadcastEvent(taskId, StockSyncStatus.FAILED, message);
		
		// 추가적으로 상세 로깅
		log.error("작업 실패 상세정보: taskId={}, errorType={}, details={}", taskId, errorType, details, exception);
	}

	// =============  핵심 메서드 =============

	private void broadcastEvent(Long taskId, StockSyncStatus status, String message) {
		try {
			// 새로운 시스템으로 위임 - taskId로부터 workerId 자동 추출
			stockSyncNotificationAdapter.broadcastStockSyncStatus(taskId, status, message);
			log.debug("SSE 이벤트 발송: taskId={}, status={}, message={}", taskId, status, message);
		} catch (Exception e) {
			log.error("SSE 이벤트 발송 실패: taskId={}, status={}, message={}", taskId, status, message, e);
		}
	}

	/**
	 * 특정 사용자에게 직접 알림 (workerId를 알고 있는 경우)
	 */
	public void broadcastEventToUser(Long taskId, Long workerId, StockSyncStatus status, String message) {
		try {
			// 사용자 ID를 직접 지정하여 전송
			stockSyncNotificationAdapter.broadcastStockSyncStatus(taskId, workerId, status, message);
			log.debug("SSE 이벤트 발송 (직접): taskId={}, workerId={}, status={}, message={}", 
					taskId, workerId, status, message);
		} catch (Exception e) {
			log.error("SSE 이벤트 발송 실패 (직접): taskId={}, workerId={}, status={}, message={}", 
					taskId, workerId, status, message, e);
		}
	}
}
