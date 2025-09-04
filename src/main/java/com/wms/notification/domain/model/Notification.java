package com.wms.notification.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 애그리거트 루트
 *
 * 실시간 SSE 전송과 추후 DB 저장을 위한 알림 도메인 모델
 * 사용자별 개인 알림과 전체 공지를 모두 처리
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 수신자 사용자 ID
     * -1L인 경우 전체 공지를 의미
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    /**
     * 알림 제목
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * 알림 메시지
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 상태 코드 (각 Status enum의 name)
     */
    @Column(name = "status_code", nullable = false)
    private String statusCode;

    /**
     * 상태 표시명
     */
    @Column(name = "status_display", nullable = false)
    private String statusDisplay;

    /**
     * 터미널 상태 여부
     */
    @Column(name = "is_terminal", nullable = false)
    private Boolean isTerminal;

    /**
     * 관련 엔티티 ID (물류작업, 재고 등)
     */
    @Column(name = "reference_id")
    private Long referenceId;

    @Builder
    private Notification(Long userId, NotificationType type, String title,
                        String message, NotificationStatus status, Long referenceId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.referenceId = referenceId;
        updateStatus(status);
    }

    /**
     * 전체 공지용 생성 메서드
     */
    public static Notification createBroadcast(NotificationType type, String title,
                                             String message, NotificationStatus status) {
        return Notification.builder()
                .userId(-1L) // 전체 공지 식별자
                .type(type)
                .title(title)
                .message(message)
                .status(status)
                .build();
    }

    /**
     * 개인 알림용 생성 메서드
     */
    public static Notification createPersonal(Long userId, NotificationType type, String title,
                                            String message, NotificationStatus status, Long referenceId) {
        return Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .status(status)
                .referenceId(referenceId)
                .build();
    }

    /**
     * 상태 업데이트
     */
    public void updateStatus(NotificationStatus status) {
        this.statusCode = status.getStatusCode();
        this.statusDisplay = status.getDisplayName();
        this.isTerminal = status.isTerminal();
    }

    /**
     * 전체 공지 여부 확인
     */
    public boolean isBroadcast() {
        return userId != null && userId.equals(-1L);
    }

    /**
     * 개인 알림 여부 확인
     */
    public boolean isPersonal() {
        return !isBroadcast();
    }
}
