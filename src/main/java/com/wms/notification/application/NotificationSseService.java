package com.wms.notification.application;

import com.wms.notification.domain.model.NotificationStatus;
import com.wms.notification.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 범용 알림 SSE 서비스
 * 사용자별 구독 관리와 실시간 알림 전송을 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseService {

    // 사용자별 구독자 관리: userId -> Set<SseEmitter>
    private final Map<Long, Set<SseEmitter>> userSubscribers = new ConcurrentHashMap<>();
    
    // 전체 공지 구독자 관리
    private final Set<SseEmitter> broadcastSubscribers = ConcurrentHashMap.newKeySet();

    /**
     * 사용자별 알림 구독
     */
    public SseEmitter subscribeToUserNotifications(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 구독자 등록
        userSubscribers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        // 연결 관리
        emitter.onCompletion(() -> removeUserSubscriber(userId, emitter));
        emitter.onTimeout(() -> removeUserSubscriber(userId, emitter));
        emitter.onError(e -> {
            log.warn("SSE 연결 에러 발생: userId={}, error={}", userId, e.getMessage());
            removeUserSubscriber(userId, emitter);
        });

        // 즉시 연결 확인 메시지 전송
        sendConnectionConfirmation(userId, emitter);

        log.info("사용자 알림 구독 등록: userId={}", userId);
        return emitter;
    }

    /**
     * 전체 공지 구독
     */
    public SseEmitter subscribeToBroadcastNotifications() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 구독자 등록
        broadcastSubscribers.add(emitter);

        // 연결 관리
        emitter.onCompletion(() -> removeBroadcastSubscriber(emitter));
        emitter.onTimeout(() -> removeBroadcastSubscriber(emitter));
        emitter.onError(e -> {
            log.warn("전체 공지 SSE 연결 에러: error={}", e.getMessage());
            removeBroadcastSubscriber(emitter);
        });

        // 즉시 연결 확인 메시지 전송
        sendBroadcastConnectionConfirmation(emitter);

        log.info("전체 공지 구독 등록");
        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendNotificationToUser(Long userId, NotificationType type, String title, 
                                     String message, NotificationStatus status, Long referenceId) {
        
        Set<SseEmitter> subscribers = userSubscribers.get(userId);
        if (subscribers == null || subscribers.isEmpty()) {
            log.debug("구독자 없음: userId={}", userId);
            return;
        }

        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .statusCode(status.getStatusCode())
                .statusDisplay(status.getDisplayName())
                .isTerminal(status.isTerminal())
                .referenceId(referenceId)
                .timestamp(LocalDateTime.now())
                .build();

        // 모든 구독자에게 전송 (실패한 연결은 자동 제거)
        subscribers.removeIf(emitter -> !sendEvent(emitter, "user-notification", event));

        log.info("사용자 알림 전송 완료: userId={}, type={}, status={}", userId, type, status.getStatusCode());

        // 터미널 상태인 경우 5초 후 연결 정리
        if (status.isTerminal() && referenceId != null) {
            scheduleConnectionCleanup(userId, referenceId, subscribers);
        }
    }

    /**
     * 전체 공지 전송
     */
    public void sendBroadcastNotification(NotificationType type, String title, 
                                        String message, NotificationStatus status) {
        
        if (broadcastSubscribers.isEmpty()) {
            log.debug("전체 공지 구독자 없음");
            return;
        }

        NotificationEvent event = NotificationEvent.builder()
                .userId(-1L) // 전체 공지 표시
                .type(type)
                .title(title)
                .message(message)
                .statusCode(status.getStatusCode())
                .statusDisplay(status.getDisplayName())
                .isTerminal(status.isTerminal())
                .timestamp(LocalDateTime.now())
                .build();

        // 모든 전체 공지 구독자에게 전송
        broadcastSubscribers.removeIf(emitter -> !sendEvent(emitter, "broadcast-notification", event));

        log.info("전체 공지 전송 완료: type={}, status={}", type, status.getStatusCode());
    }

    /**
     * 구독자 제거 - 사용자별
     */
    public void removeUserSubscriber(Long userId, SseEmitter emitter) {
        Set<SseEmitter> subscribers = userSubscribers.get(userId);
        if (subscribers != null) {
            subscribers.remove(emitter);
            if (subscribers.isEmpty()) {
                userSubscribers.remove(userId);
                log.debug("사용자 구독 정보 정리: userId={}", userId);
            }
        }
    }

    /**
     * 구독자 제거 - 전체 공지
     */
    public void removeBroadcastSubscriber(SseEmitter emitter) {
        broadcastSubscribers.remove(emitter);
    }

    /**
     * 연결 확인 메시지 전송
     */
    private void sendConnectionConfirmation(Long userId, SseEmitter emitter) {
        try {
            NotificationEvent confirmEvent = NotificationEvent.builder()
                    .userId(userId)
                    .type(NotificationType.SYSTEM)
                    .title("연결 확인")
                    .message("알림 서비스에 연결되었습니다.")
                    .statusCode("CONNECTED")
                    .statusDisplay("연결됨")
                    .isTerminal(false)
                    .timestamp(LocalDateTime.now())
                    .build();

            emitter.send(SseEmitter.event()
                    .name("connection-confirmed")
                    .data(confirmEvent));

        } catch (Exception e) {
            log.warn("연결 확인 메시지 전송 실패: userId={}", userId);
            removeUserSubscriber(userId, emitter);
        }
    }

    /**
     * 전체 공지 연결 확인 메시지 전송
     */
    private void sendBroadcastConnectionConfirmation(SseEmitter emitter) {
        try {
            NotificationEvent confirmEvent = NotificationEvent.builder()
                    .userId(-1L)
                    .type(NotificationType.SYSTEM)
                    .title("전체 공지 연결")
                    .message("전체 공지 서비스에 연결되었습니다.")
                    .statusCode("CONNECTED")
                    .statusDisplay("연결됨")
                    .isTerminal(false)
                    .timestamp(LocalDateTime.now())
                    .build();

            emitter.send(SseEmitter.event()
                    .name("broadcast-connection-confirmed")
                    .data(confirmEvent));

        } catch (Exception e) {
            log.warn("전체 공지 연결 확인 메시지 전송 실패");
            removeBroadcastSubscriber(emitter);
        }
    }

    /**
     * SSE 이벤트 전송
     */
    private boolean sendEvent(SseEmitter emitter, String eventName, NotificationEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(event));
            return true;
        } catch (Exception e) {
            log.debug("SSE 이벤트 전송 실패: {}", e.getMessage());
            return false; // 전송 실패시 제거 대상
        }
    }

    /**
     * 터미널 상태 도달시 연결 정리 스케줄링
     */
    private void scheduleConnectionCleanup(Long userId, Long referenceId, Set<SseEmitter> subscribers) {
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
                .execute(() -> {
                    // 종료 알림 전송
                    NotificationEvent closeEvent = NotificationEvent.builder()
                            .userId(userId)
                            .type(NotificationType.SYSTEM)
                            .title("작업 완료")
                            .message("작업이 완료되어 연결을 종료합니다.")
                            .statusCode("DISCONNECTING")
                            .statusDisplay("연결 종료")
                            .isTerminal(true)
                            .referenceId(referenceId)
                            .timestamp(LocalDateTime.now())
                            .build();

                    subscribers.forEach(emitter -> {
                        sendEvent(emitter, "connection-closing", closeEvent);
                        try {
                            emitter.complete();
                        } catch (Exception e) {
                            log.debug("연결 종료 중 오류: {}", e.getMessage());
                        }
                    });

                    log.debug("터미널 상태 연결 정리 완료: userId={}, referenceId={}", userId, referenceId);
                });
    }

    /**
     * 현재 구독 상태 조회 (디버깅/모니터링용)
     */
    public Map<String, Object> getSubscriptionStatus() {
        return Map.of(
            "userSubscribers", userSubscribers.size(),
            "broadcastSubscribers", broadcastSubscribers.size(),
            "userSubscriberDetails", userSubscribers.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                ))
        );
    }

    /**
     * 알림 이벤트 데이터 클래스
     */
    @lombok.Builder
    @lombok.Getter
    public static class NotificationEvent {
        private Long userId;
        private NotificationType type;
        private String title;
        private String message;
        private LocalDateTime timestamp;
        private String statusCode;
        private String statusDisplay;
        private Boolean isTerminal;
        private Long referenceId;
    }
}
