package com.wms.stock.interfaces;

import com.wms.stock.application.StockSyncEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 재고 동기화 상태 SSE 컨트롤러
 * 물류 작업의 재고 동기화 진행 상황을 실시간으로 클라이언트에게 전송
 */
@RestController
@RequestMapping("/stock-sync-events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "StockSyncEvents", description = "재고 동기화 실시간 이벤트 API")
public class StockSyncEventController {

    private final StockSyncEventService stockSyncEventService;

    /**
     * 특정 물류 작업의 재고 동기화 상태 구독
     */
    @GetMapping(value = "/subscribe/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "재고 동기화 상태 실시간 구독", 
        description = "특정 물류 작업의 재고 동기화 진행 상황을 실시간으로 스트리밍합니다."
    )
    @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공")
    public SseEmitter subscribeToStockSyncEvents(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("재고 동기화 상태 구독 요청: taskId={}", taskId);
        
        SseEmitter emitter = stockSyncEventService.subscribeToStockSyncEvents(taskId);
        
        log.debug("SSE 연결 생성 완료: taskId={}", taskId);
        return emitter;
    }

    /**
     * 구독 해제 (클라이언트에서 명시적으로 호출)
     */
    @DeleteMapping("/unsubscribe/{taskId}")
    @Operation(
        summary = "재고 동기화 상태 구독 해제", 
        description = "특정 물류 작업의 재고 동기화 상태 구독을 해제합니다."
    )
    @ApiResponse(responseCode = "200", description = "구독 해제 성공")
    public void unsubscribeFromStockSyncEvents(
            @Parameter(description = "물류 작업 ID") @PathVariable Long taskId) {
        
        log.info("재고 동기화 상태 구독 해제 요청: taskId={}", taskId);
        
        // 실제로는 클라이언트가 연결을 끊으면 자동으로 정리되지만,
        // 명시적 해제 요청이 있을 때 처리할 수 있는 엔드포인트
        // 현재는 자동 정리에 의존하므로 별도 작업 없음
        
        log.debug("구독 해제 처리 완료: taskId={}", taskId);
    }

    /**
     * 서버 상태 확인용 헬스 체크
     */
    @GetMapping("/health")
    @Operation(summary = "SSE 서버 상태 확인", description = "재고 동기화 이벤트 서버의 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "서버 정상")
    public String healthCheck() {
        return "SSE Stock Sync Event Server is running";
    }
}
