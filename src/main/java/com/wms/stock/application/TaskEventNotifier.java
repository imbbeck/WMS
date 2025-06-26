//package com.wms.stock.application;
//
//import com.wms.stock.domain.event.StockSyncStatus;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TaskEventNotifier {
//
//	private final StockSyncEventService stockSyncEventService;
//
//	// =============  작업 라이프사이클 이벤트 =============
//
//	public void notifyTaskInitiationStarted(Long taskId) {
//		broadcastEvent(taskId, StockSyncStatus.PROCESSING, "작업 시작 처리 중...");
//	}
//
//	public void notifyTaskInitiated(Long taskId) {
//		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업이 시작되었습니다");
//	}
//
//	public void notifyTaskInitiatedWithoutStock(Long taskId, String reason) {
//		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업 시작 완료 (" + reason + ")");
//	}
//
//	public void notifyTaskCompletionStarted(Long taskId) {
//		broadcastEvent(taskId, StockSyncStatus.PROCESSING, "작업 완료 처리 중...");
//	}
//
//	public void notifyTaskCompleted(Long taskId) {
//		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업이 완료되었습니다");
//	}
//
//	public void notifyTaskCompletedWithoutStock(Long taskId, String reason) {
//		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "작업 완료 (" + reason + ")");
//	}
//
//	// =============  재고 동기화 이벤트 =============
//
//	public void notifyStockSyncQueued(Long taskId, String details) {
//		broadcastEvent(taskId, StockSyncStatus.QUEUED, "재고 동기화 대기: " + details);
//	}
//
//	public void notifyStockSyncProcessing(Long taskId, String operation) {
//		broadcastEvent(taskId, StockSyncStatus.PROCESSING, "재고 " + operation + " 진행 중...");
//	}
//
//	public void notifyStockSyncRetrying(Long taskId, int retryCount) {
//		broadcastEvent(taskId, StockSyncStatus.RETRYING,
//				String.format("동시성 충돌로 재시도 중... (%d회)", retryCount));
//	}
//
//	public void notifyStockSyncCompleted(Long taskId) {
//		broadcastEvent(taskId, StockSyncStatus.COMPLETED, "재고 동기화가 완료되었습니다");
//	}
//
//	// =============  오류 이벤트 =============
//
//	public void notifyValidationFailed(Long taskId, String reason) {
//		broadcastEvent(taskId, StockSyncStatus.FAILED, "검증 실패: " + reason);
//	}
//
//	public void notifyCapacityExceeded(Long taskId, Long warehouseId, int current, int requested, int capacity) {
//		String message = String.format("창고(%d) 용량 초과: 현재 %d + 요청 %d > 한계 %d",
//				warehouseId, current, requested, capacity);
//		broadcastEvent(taskId, StockSyncStatus.FAILED, message);
//	}
//
//	public void notifyInsufficientStock(Long taskId, Long warehouseId, Long wareId, int current, int requested) {
//		String message = String.format("재고 부족: 창고(%d) 물품(%d) 현재 %d < 요청 %d",
//				warehouseId, wareId, current, requested);
//		broadcastEvent(taskId, StockSyncStatus.FAILED, message);
//	}
//
//	public void notifyStreamPublishFailed(Long taskId, String streamKey, String error) {
//		broadcastEvent(taskId, StockSyncStatus.FAILED,
//				String.format("스트림 발행 실패(%s): %s", streamKey, error));
//	}
//
//	public void notifyProcessingError(Long taskId, String error) {
//		broadcastEvent(taskId, StockSyncStatus.FAILED, "처리 오류: " + error);
//	}
//
//	// =============  핵심 메서드 (빠진 부분!) =============
//
//	private void broadcastEvent(Long taskId, StockSyncStatus status, String message) {
//		try {
//			stockSyncEventService.broadcastSyncStatus(taskId, status, message);
//			log.debug("SSE 이벤트 발송: taskId={}, status={}, message={}", taskId, status, message);
//		} catch (Exception e) {
//			log.error("SSE 이벤트 발송 실패: taskId={}, status={}, message={}", taskId, status, message, e);
//		}
//	}
//}