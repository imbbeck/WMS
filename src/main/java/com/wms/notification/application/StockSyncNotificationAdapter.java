package com.wms.notification.application;

import com.wms.logisticTask.domain.repository.LogisticTaskRepository;
import com.wms.notification.domain.model.NotificationType;
import com.wms.notification.domain.model.StockSyncStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기존 재고 동기화 SSE 서비스와 새로운 범용 알림 서비스를 연결하는 어댑터
 * 
 * 기존 StockSyncEventService의 API를 유지하면서 
 * 내부적으로는 새로운 NotificationSseService를 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockSyncNotificationAdapter {

    private final NotificationSseService notificationSseService;
    private final LogisticTaskRepository logisticTaskRepository;

    /**
     * 재고 동기화 상태 업데이트를 새로운 알림 시스템으로 전송
     * 
     * @param taskId 물류 작업 ID
     * @param userId 작업 담당 사용자 ID  
     * @param status 재고 동기화 상태
     * @param message 상태 메시지
     */
    public void broadcastStockSyncStatus(Long taskId, Long userId, StockSyncStatus status, String message) {
        String title = generateStockSyncTitle(taskId, status);
        
        notificationSseService.sendNotificationToUser(
            userId,
            NotificationType.STOCK_SYNC,
            title,
            message,
            status,
            taskId
        );
        
        log.debug("재고 동기화 알림 전송 완료: taskId={}, userId={}, status={}", taskId, userId, status);
    }

    /**
     * 기존 API 호환성을 위한 taskId 중심 메서드
     * taskId로부터 workerId를 추출하여 전송
     */
    public void broadcastStockSyncStatus(Long taskId, StockSyncStatus status, String message) {
        Long workerId = extractWorkerIdFromTaskId(taskId);
        
        if (workerId != null) {
            broadcastStockSyncStatus(taskId, workerId, status, message);
        } else {
            log.warn("TaskId {}에 대한 workerId를 찾을 수 없어 알림 전송을 건너뜁니다.", taskId);
        }
    }

    /**
     * 재고 동기화 상태에 따른 제목 생성
     */
    private String generateStockSyncTitle(Long taskId, StockSyncStatus status) {
        return switch (status) {
            case WAITING -> "재고 동기화 대기";
            case QUEUED -> "재고 동기화 대기열 등록";
            case PROCESSING -> "재고 동기화 진행 중";
            case RETRYING -> "재고 동기화 재시도";
            case COMPLETED -> "재고 동기화 완료";
            case FAILED -> "재고 동기화 실패";
        } + " - 작업 #" + taskId;
    }

    /**
     * TaskId로부터 WorkerId 추출
     * LogisticTaskRepository를 통해 실제 workerId 조회
     */
    private Long extractWorkerIdFromTaskId(Long taskId) {
        try {
            return logisticTaskRepository.findById(taskId)
                    .map(task -> task.getWorker().getId())
                    .orElse(null);
        } catch (Exception e) {
            log.error("TaskId {}로부터 workerId 추출 중 에러 발생: {}", taskId, e.getMessage());
            return null;
        }
    }
}
