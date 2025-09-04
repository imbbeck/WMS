package com.wms.notification.domain.model;

/**
 * 시스템 알림 상태
 * 시스템 공지, 점검, 업데이트 등의 상태를 관리
 */
public enum SystemNotificationStatus implements NotificationStatus {
    SCHEDULED("예정"),
    ACTIVE("진행 중"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    private final String displayName;

    SystemNotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.SYSTEM;
    }
}
