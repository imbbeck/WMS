package com.wms.notification.domain.model;

/**
 * 작업 알림 상태
 * 물류 작업의 진행 상황이나 작업자 배정 등의 상태를 관리
 */
public enum TaskNotificationStatus implements NotificationStatus {
    ASSIGNED("배정됨"),
    IN_PROGRESS("진행 중"),
    DELAYED("지연됨"),
    COMPLETED("완료"),
    FAILED("실패"),
    REASSIGNED("재배정됨");

    private final String displayName;

    TaskNotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.TASK;
    }
}
