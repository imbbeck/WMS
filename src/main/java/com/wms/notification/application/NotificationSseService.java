package com.wms.notification.application;

import com.wms.notification.domain.model.NotificationStatus;
import com.wms.notification.domain.model.NotificationType;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 범용 알림 SSE 서비스
 * 사용자별 구독 관리와 실시간 알림 전송을 담당
 * ADMIN 유저는 모든 알림을 수신할 수 있음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseService {

    // 사용자별 구독자 관리: userId -> Set<SseEmitter>
    private final Map<Long, Set<SseEmitter>> userSubscribers = new ConcurrentHashMap<>();
    
    private final UserInfoRepository userInfoRepository;

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
     * 특정 사용자에게 알림 전송
     * ADMIN 유저들도 모든 알림을 수신함
     */
    public void sendNotificationToUser(Long userId, NotificationType type, String title, 
                                     String message, NotificationStatus status, Long referenceId) {
        
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

        // 1. 대상 사용자에게 전송
        sendToSpecificUser(userId, event);

        // 2. 모든 ADMIN 유저에게도 전송 (대상 사용자가 ADMIN이 아닌 경우만)
        sendToAllAdmins(userId, event);

        log.info("사용자 알림 전송 완료: userId={}, type={}, status={}", userId, type, status.getStatusCode());
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    private void sendToSpecificUser(Long userId, NotificationEvent event) {
        Set<SseEmitter> subscribers = userSubscribers.get(userId);
        if (subscribers == null || subscribers.isEmpty()) {
            log.debug("구독자 없음: userId={}", userId);
            return;
        }

        // 모든 구독자에게 전송 (실패한 연결은 자동 제거)
        subscribers.removeIf(emitter -> !sendEvent(emitter, "user-notification", event));
        log.debug("특정 사용자 알림 전송: userId={}", userId);
    }

    /**
     * 모든 ADMIN 유저에게 알림 전송 (원래 대상자가 ADMIN이 아닌 경우만)
     */
    private void sendToAllAdmins(Long originalUserId, NotificationEvent event) {
        try {
            // 원래 대상자가 ADMIN인지 확인
            UserInfo originalUser = userInfoRepository.findById(originalUserId).orElse(null);
            if (originalUser != null && originalUser.getType() == UserType.ADMIN) {
                log.debug("원래 대상자가 ADMIN이므로 중복 전송 생략: userId={}", originalUserId);
                return;
            }

            // 모든 ADMIN 유저 조회
            Set<Long> adminUserIds = userInfoRepository.findAllByType(UserType.ADMIN)
                    .stream()
                    .map(UserInfo::getId)
                    .collect(Collectors.toSet());

            // ADMIN 이벤트 생성 (ADMIN용 메시지 표시)
            NotificationEvent adminEvent = NotificationEvent.builder()
                    .userId(-1L)  // ADMIN 모니터링 표시
                    .type(event.getType())
                    .title("[모니터링] " + event.getTitle())
		            .message(String.format("[사용자 ID: %d%s] %s",
				            originalUserId,
				            event.getReferenceId() != null ? ", 작업 ID: " + event.getReferenceId() : "",
				            event.getMessage()))
                    .statusCode(event.getStatusCode())
                    .statusDisplay(event.getStatusDisplay())
                    .isTerminal(event.getIsTerminal())
                    .referenceId(event.getReferenceId())
                    .timestamp(event.getTimestamp())
                    .build();

            // 각 ADMIN에게 전송
            for (Long adminId : adminUserIds) {
                Set<SseEmitter> adminSubscribers = userSubscribers.get(adminId);
                if (adminSubscribers != null && !adminSubscribers.isEmpty()) {
                    adminSubscribers.removeIf(emitter -> !sendEvent(emitter, "admin-monitoring", adminEvent));
                }
            }

            log.debug("ADMIN 모니터링 알림 전송 완료: 대상 ADMIN 수={}", adminUserIds.size());

        } catch (Exception e) {
            log.warn("ADMIN 모니터링 알림 전송 실패: {}", e.getMessage());
        }
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
     * 연결 확인 메시지 전송
     */
    private void sendConnectionConfirmation(Long userId, SseEmitter emitter) {
        try {
            // 사용자 타입 확인
            UserInfo user = userInfoRepository.findById(userId).orElse(null);
            String userTypeMessage = (user != null && user.getType() == UserType.ADMIN) 
                ? "관리자 알림 서비스에 연결되었습니다. (모든 알림 수신)"
                : "알림 서비스에 연결되었습니다.";

            NotificationEvent confirmEvent = NotificationEvent.builder()
                    .userId(userId)
                    .type(NotificationType.SYSTEM)
                    .title("연결 확인")
                    .message(userTypeMessage)
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

	@Scheduled(fixedRate = 30000) // 30초마다 실행
	public void sendHeartbeat() {
		if (userSubscribers.isEmpty()) {
			log.trace("구독자 없음, 하트비트 스킵");
			return;
		}

		// 모든 구독자에게 하트비트 전송
		userSubscribers.forEach((userId, emitters) -> {
			emitters.removeIf(emitter -> {
				try {
					// 주석(comment) 형태의 더미 데이터 전송
					emitter.send(SseEmitter.event().comment("heartbeat"));
					log.trace("하트비트 전송 성공: userId={}", userId);
					return false; // 연결 유지
				} catch (IOException e) {
					log.debug("하트비트 전송 실패, 구독자 제거: userId={}, error={}", userId, e.getMessage());
					return true; // 연결 실패, 제거 대상
				}
			});
		});
	}

    /**
     * 사용자의 모든 SSE 연결 종료 (로그아웃 시 호출)
     */
    public void disconnectAllUserConnections(Long userId) {
        Set<SseEmitter> subscribers = userSubscribers.get(userId);
        if (subscribers != null) {
            subscribers.forEach(emitter -> {
                try {
                    emitter.complete();
                    log.debug("SSE 연결 종료: userId={}", userId);
                } catch (Exception e) {
                    log.debug("SSE 연결 종료 중 오류 (무시 가능): userId={}", userId, e);
                }
            });

            userSubscribers.remove(userId);
            log.info("사용자 모든 SSE 연결 정리 완료: userId={}, 연결 수={}", userId, subscribers.size());
        }
    }

    /**
     * 현재 구독 상태 조회 (디버깅/모니터링용)
     */
    public Map<String, Object> getSubscriptionStatus() {
        return Map.of(
            "userSubscribers", userSubscribers.size(),
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
