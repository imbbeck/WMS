//package com.wms.stock.interfaces;
//
//import com.wms.stock.application.StockSyncEventService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//@RestController
//@RequestMapping("/api/tasks")
//@RequiredArgsConstructor
//@Slf4j
//@Tag(name = "Stock Sync Events", description = "재고 동기화 상태 실시간 이벤트 API")
//public class StockSyncEventController {
//
//	private final StockSyncEventService stockSyncEventService;
//
//	@GetMapping(value = "/{taskId}/stock-sync-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//	@Operation(
//			summary = "재고 동기화 상태 실시간 구독",
//			description = "물류 작업의 재고 동기화 진행 상황을 실시간으로 받아볼 수 있습니다."
//	)
//	public SseEmitter subscribeToStockSyncEvents(
//			@Parameter(description = "물류 작업 ID", example = "123")
//			@PathVariable Long taskId) {
//
//		log.info("SSE 구독 요청: taskId={}", taskId);
//
//		try {
//			SseEmitter emitter = stockSyncEventService.subscribeToStockSyncEvents(taskId);
//
//			log.debug("SSE 구독 성공: taskId={}", taskId);
//			return emitter;
//
//		} catch (Exception e) {
//			log.error("SSE 구독 실패: taskId={}", taskId, e);
//
//			// 에러 시에도 SseEmitter 반환 (에러 메시지 전송 후 종료)
//			SseEmitter errorEmitter = new SseEmitter(1000L);
//			try {
//				errorEmitter.send(SseEmitter.event()
//						.name("error")
//						.data("구독 중 오류가 발생했습니다: " + e.getMessage()));
//				errorEmitter.complete();
//			} catch (Exception sendError) {
//				log.error("에러 메시지 전송 실패", sendError);
//			}
//
//			return errorEmitter;
//		}
//	}
//
//	@GetMapping("/{taskId}/sync-status")
//	@Operation(
//			summary = "재고 동기화 현재 상태 조회",
//			description = "SSE 대신 폴링으로 현재 상태를 확인할 수 있습니다."
//	)
//	public ResponseEntity<SyncStatusResponse> getCurrentSyncStatus(
//			@Parameter(description = "물류 작업 ID", example = "123")
//			@PathVariable Long taskId) {
//
//		// 현재는 단순 구현 (필요시 확장)
//		log.debug("동기화 상태 조회: taskId={}", taskId);
//
//		// 실제로는 Redis나 DB에서 현재 상태를 조회
//		SyncStatusResponse response = SyncStatusResponse.builder()
//				.taskId(taskId)
//				.message("SSE 구독을 통해 실시간 상태를 확인하세요")
//				.timestamp(java.time.LocalDateTime.now())
//				.build();
//
//		return ResponseEntity.ok(response);
//	}
//
//	// 내부 응답 DTO
//	@lombok.Builder
//	@lombok.Getter
//	public static class SyncStatusResponse {
//		private Long taskId;
//		private String message;
//		private java.time.LocalDateTime timestamp;
//	}
//}
