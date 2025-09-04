package com.wms.notification.domain.model;

/**
 * 알림 상태 전략 패턴 인터페이스
 * 각 알림 타입별 고유한 상태값과 메타데이터를 제공
 */
public interface NotificationStatus {
    
    /**
     * 화면에 표시될 상태명
     */
    String getDisplayName();
    
    /**
     * 터미널 상태 여부 (완료/실패로 더 이상 상태 변경이 없는 상태)
     */
    boolean isTerminal();
    
    /**
     * 알림 타입
     */
    NotificationType getNotificationType();
    
    /**
     * 상태 코드 (enum name과 동일)
     */
    default String getStatusCode() {
        return this.toString();
    }
}
