package com.wms.applicationInfra.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * 에러 메시지
     */
    private String message;
    
    /**
     * 에러 코드 (도메인별 구분)
     */
    private String errorCode;
    
    /**
     * HTTP 상태 코드
     */
    private int status;
    
    /**
     * 에러 발생 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 요청 경로
     */
    private String path;
    
    /**
     * 상세 에러 정보 (필드별 검증 에러 등)
     */
    private List<FieldError> fieldErrors;
    
    /**
     * 추가 메타데이터
     */
    private Map<String, Object> metadata;
    
    /**
     * 기본 생성자 (간단한 에러 응답용)
     */
    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 에러 코드와 함께 생성
     */
    public ErrorResponse(String message, String errorCode) {
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 상태 코드와 함께 생성
     */
    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 필드 에러 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String value;
        private String reason;
    }
    
    /**
     * 에러 코드 상수 정의
     */
    public static class ErrorCodes {
        // UserInfo 도메인
        public static final String USER_NOT_FOUND = "USER_001";
        public static final String USER_DUPLICATE_USERNAME = "USER_002";
        public static final String USER_DUPLICATE_EMAIL = "USER_003";
        public static final String USER_INVALID_USERNAME = "USER_004";
        public static final String USER_WRONG_PASSWORD = "USER_005";
        public static final String USER_UNAUTHORIZED_WITHDRAW = "USER_006";
        
        // Ware 도메인
        public static final String WARE_NOT_FOUND = "WARE_001";
        public static final String WARE_DUPLICATE_NAME = "WARE_002";
        public static final String WARE_INVALID_NAME = "WARE_003";
        public static final String WARE_INVALID_TYPE = "WARE_004";
        public static final String WARE_INVALID_PALETTE_UNIT = "WARE_005";
        
        // Location 도메인
        public static final String LOCATION_NOT_FOUND = "LOCATION_001";
        public static final String LOCATION_DUPLICATE_NAME = "LOCATION_002";
        public static final String LOCATION_INVALID_TYPE = "LOCATION_003";
        public static final String LOCATION_CAPACITY_EXCEEDED = "LOCATION_004";
        
        // Stock 도메인
        public static final String STOCK_NOT_FOUND = "STOCK_001";
        public static final String STOCK_INSUFFICIENT = "STOCK_002";
        public static final String STOCK_CAPACITY_EXCEEDED = "STOCK_003";
        public static final String STOCK_INVALID_QUANTITY = "STOCK_004";
        
        // LogisticTemplate 도메인
        public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_001";
        public static final String TEMPLATE_INVALID_NAME = "TEMPLATE_002";
        public static final String TEMPLATE_INVALID_TYPE = "TEMPLATE_003";
        public static final String TEMPLATE_INVALID_QUANTITY = "TEMPLATE_004";
        public static final String TEMPLATE_WARE_NOT_FOUND = "TEMPLATE_005";
        public static final String TEMPLATE_LOCATION_NOT_FOUND = "TEMPLATE_006";
        
        // LogisticTask 도메인
        public static final String TASK_NOT_MODIFIABLE = "TASK_001";
        public static final String TASK_CANCELLATION_NOT_ALLOWED = "TASK_002";
        public static final String TASK_FAILURE_NOT_ALLOWED = "TASK_003";
        public static final String TASK_INVALID_DATA = "TASK_004";
        
        // 공통
        public static final String VALIDATION_ERROR = "VALIDATION_001";
        public static final String INTERNAL_SERVER_ERROR = "SYSTEM_001";
        public static final String UNAUTHORIZED = "AUTH_001";
        public static final String FORBIDDEN = "AUTH_002";
    }
} 