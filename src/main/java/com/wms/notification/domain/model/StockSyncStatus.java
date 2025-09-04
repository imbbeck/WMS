package com.wms.notification.domain.model;

/**
 * 재고 동기화 상태
 * NotificationStatus 전략 패턴 구현체
 */
public enum StockSyncStatus implements NotificationStatus {
    WAITING("대기 중"),
    QUEUED("대기열 등록"),
    PROCESSING("처리 중"),
    RETRYING("재시도 중"),
    COMPLETED("완료"),
    FAILED("실패");

    private final String displayName;

    StockSyncStatus(String displayName) {
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
        return NotificationType.STOCK_SYNC;
    }
}
