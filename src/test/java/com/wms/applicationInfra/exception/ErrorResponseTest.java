package com.wms.applicationInfra.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("에러 응답 테스트")
class ErrorResponseTest {

    @Test
    @DisplayName("기본 생성자로 에러 응답을 생성할 수 있다")
    void testBasicConstructor() {
        ErrorResponse errorResponse = new ErrorResponse("테스트 에러 메시지");

        assertThat(errorResponse.getMessage()).isEqualTo("테스트 에러 메시지");
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getErrorCode()).isNull();
        assertThat(errorResponse.getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("에러 코드와 함께 에러 응답을 생성할 수 있다")
    void testConstructorWithErrorCode() {
        ErrorResponse errorResponse = new ErrorResponse("테스트 에러 메시지", "TEST_001");

        assertThat(errorResponse.getMessage()).isEqualTo("테스트 에러 메시지");
        assertThat(errorResponse.getErrorCode()).isEqualTo("TEST_001");
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("상태 코드와 함께 에러 응답을 생성할 수 있다")
    void testConstructorWithStatus() {
        ErrorResponse errorResponse = new ErrorResponse("테스트 에러 메시지", 400);

        assertThat(errorResponse.getMessage()).isEqualTo("테스트 에러 메시지");
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("빌더 패턴으로 완전한 에러 응답을 생성할 수 있다")
    void testBuilderPattern() {
        LocalDateTime now = LocalDateTime.now();
        List<ErrorResponse.FieldError> fieldErrors = List.of(
                ErrorResponse.FieldError.builder()
                        .field("username")
                        .value("test")
                        .reason("사용자명은 필수입니다")
                        .build()
        );
        Map<String, Object> metadata = Map.of("requestId", "12345");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("입력값이 올바르지 않습니다")
                .errorCode(ErrorResponse.ErrorCodes.VALIDATION_ERROR)
                .status(400)
                .timestamp(now)
                .path("/api/users")
                .fieldErrors(fieldErrors)
                .metadata(metadata)
                .build();

        assertThat(errorResponse.getMessage()).isEqualTo("입력값이 올바르지 않습니다");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorResponse.ErrorCodes.VALIDATION_ERROR);
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getTimestamp()).isEqualTo(now);
        assertThat(errorResponse.getPath()).isEqualTo("/api/users");
        assertThat(errorResponse.getFieldErrors()).hasSize(1);
        assertThat(errorResponse.getFieldErrors().get(0).getField()).isEqualTo("username");
        assertThat(errorResponse.getFieldErrors().get(0).getValue()).isEqualTo("test");
        assertThat(errorResponse.getFieldErrors().get(0).getReason()).isEqualTo("사용자명은 필수입니다");
        assertThat(errorResponse.getMetadata()).containsEntry("requestId", "12345");
    }

    @Test
    @DisplayName("필드 에러를 생성할 수 있다")
    void testFieldError() {
        ErrorResponse.FieldError fieldError = ErrorResponse.FieldError.builder()
                .field("email")
                .value("invalid-email")
                .reason("유효한 이메일 형식이 아닙니다")
                .build();

        assertThat(fieldError.getField()).isEqualTo("email");
        assertThat(fieldError.getValue()).isEqualTo("invalid-email");
        assertThat(fieldError.getReason()).isEqualTo("유효한 이메일 형식이 아닙니다");
    }

    @Test
    @DisplayName("에러 코드 상수들이 올바르게 정의되어 있다")
    void testErrorCodes() {
        // UserInfo 도메인
        assertThat(ErrorResponse.ErrorCodes.USER_NOT_FOUND).isEqualTo("USER_001");
        assertThat(ErrorResponse.ErrorCodes.USER_DUPLICATE_USERNAME).isEqualTo("USER_002");
        assertThat(ErrorResponse.ErrorCodes.USER_DUPLICATE_EMAIL).isEqualTo("USER_003");

        // Ware 도메인
        assertThat(ErrorResponse.ErrorCodes.WARE_NOT_FOUND).isEqualTo("WARE_001");
        assertThat(ErrorResponse.ErrorCodes.WARE_DUPLICATE_NAME).isEqualTo("WARE_002");

        // Location 도메인
        assertThat(ErrorResponse.ErrorCodes.LOCATION_NOT_FOUND).isEqualTo("LOCATION_001");
        assertThat(ErrorResponse.ErrorCodes.LOCATION_DUPLICATE_NAME).isEqualTo("LOCATION_002");

        // Stock 도메인
        assertThat(ErrorResponse.ErrorCodes.STOCK_NOT_FOUND).isEqualTo("STOCK_001");
        assertThat(ErrorResponse.ErrorCodes.STOCK_INSUFFICIENT).isEqualTo("STOCK_002");

        // LogisticTemplate 도메인
        assertThat(ErrorResponse.ErrorCodes.TEMPLATE_NOT_FOUND).isEqualTo("TEMPLATE_001");
        assertThat(ErrorResponse.ErrorCodes.TEMPLATE_INVALID_NAME).isEqualTo("TEMPLATE_002");

        // LogisticTask 도메인
        assertThat(ErrorResponse.ErrorCodes.TASK_NOT_MODIFIABLE).isEqualTo("TASK_001");
        assertThat(ErrorResponse.ErrorCodes.TASK_CANCELLATION_NOT_ALLOWED).isEqualTo("TASK_002");

        // 공통
        assertThat(ErrorResponse.ErrorCodes.VALIDATION_ERROR).isEqualTo("VALIDATION_001");
        assertThat(ErrorResponse.ErrorCodes.INTERNAL_SERVER_ERROR).isEqualTo("SYSTEM_001");
        assertThat(ErrorResponse.ErrorCodes.UNAUTHORIZED).isEqualTo("AUTH_001");
        assertThat(ErrorResponse.ErrorCodes.FORBIDDEN).isEqualTo("AUTH_002");
    }
} 