//package com.wms.stock.application;
//
//import com.wms.stock.domain.event.StockSyncStatus;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class StockSyncEventService {
//
//	// taskId별 구독자 관리
//	private final Map<Long, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();
//
//	public SseEmitter subscribeToStockSyncEvents(Long taskId) {
//		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//
//		// 구독자 등록
//		subscribers.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
//
//		// 연결 관리
//		emitter.onCompletion(() -> removeSubscriber(taskId, emitter));
//		emitter.onTimeout(() -> removeSubscriber(taskId, emitter));
//		emitter.onError(e -> removeSubscriber(taskId, emitter));
//
//		// 즉시 초기 상태 전송
//		sendInitialStatus(taskId, emitter);
//
//		return emitter;
//	}
//
//	public void removeSubscriber(Long taskId, SseEmitter emitter) {
//		Set<SseEmitter> taskSubscribers = subscribers.get(taskId);
//		if (taskSubscribers != null) {
//			taskSubscribers.remove(emitter);
//			if (taskSubscribers.isEmpty()) {
//				subscribers.remove(taskId);
//			}
//		}
//	}
//
//	private void sendInitialStatus(Long taskId, SseEmitter emitter) {
//		try {
//			StockSyncEvent initialEvent = StockSyncEvent.builder()
//					.taskId(taskId)
//					.status(StockSyncStatus.WAITING)
//					.message("재고 동기화 대기 중...")
//					.timestamp(LocalDateTime.now())
//					.build();
//
//			emitter.send(SseEmitter.event()
//					.name("stock-sync-status")
//					.data(initialEvent));
//
//		} catch (Exception e) {
//			removeSubscriber(taskId, emitter);
//		}
//	}
//
//	// 상태 업데이트 브로드캐스트
//	public void broadcastSyncStatus(Long taskId, StockSyncStatus status, String message) {
//		Set<SseEmitter> taskSubscribers = subscribers.get(taskId);
//
//		if (taskSubscribers != null && !taskSubscribers.isEmpty()) {
//			StockSyncEvent event = StockSyncEvent.builder()
//					.taskId(taskId)
//					.status(status)
//					.message(message)
//					.timestamp(LocalDateTime.now())
//					.build();
//
//			// 모든 구독자에게 전송
//			taskSubscribers.removeIf(emitter -> !sendEvent(emitter, event));
//
//			// 완료/실패 시 5초 후 구독자 정리
//			if (status.isTerminal()) {
//				CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
//						.execute(() -> subscribers.remove(taskId));
//			}
//		}
//	}
//
//	private boolean sendEvent(SseEmitter emitter, StockSyncEvent event) {
//		try {
//			emitter.send(SseEmitter.event()
//					.name("stock-sync-status")
//					.data(event));
//			return true;
//		} catch (Exception e) {
//			return false; // 전송 실패시 제거 대상
//		}
//	}
//
//	// Inner class for event data
//	@lombok.Builder
//	@lombok.Getter
//	public static class StockSyncEvent {
//		private Long taskId;
//		private StockSyncStatus status;
//		private String message;
//		private LocalDateTime timestamp;
//	}
//}