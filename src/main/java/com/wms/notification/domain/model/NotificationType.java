package com.wms.notification.domain.model;

/**
 * 알림 타입 분류
 */
public enum NotificationType {
    /**
     * 재고 동기화 관련 알림
     */
    STOCK_SYNC("재고 동기화"),
    
    /**
     * 시스템 공지 알림
     */
    SYSTEM("시스템 알림"),
    
    /**
     * 물류 작업 관련 알림
     */
    TASK("작업 알림");
    
    private final String displayName;
    
    NotificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
