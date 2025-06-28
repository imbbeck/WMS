package com.wms.applicationInfra.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile; // 현재 활성화된 프로파일을 가져옵니다.

    /**
     * @Valid 어노테이션을 사용한 DTO의 필드 유효성 검사 실패 시 발생하는 예외를 처리합니다.
     * @param ex MethodArgumentNotValidException
     * @param request 웹 요청
     * @return 상세 필드 오류 정보를 포함하는 ErrorResponse
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error occurred: {}", ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .value(error.getRejectedValue() != null ? error.getRejectedValue().toString() : null)
                        .reason(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("입력값이 올바르지 않습니다.")
                .errorCode(ErrorResponse.ErrorCodes.VALIDATION_ERROR)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 직접 정의한 모든 비즈니스 예외를 처리합니다.
     * @param ex BusinessException 인터페이스를 구현한 예외
     * @param request 웹 요청
     * @return ErrorResponse를 포함하는 ResponseEntity
     */
    @ExceptionHandler(BusinessException.AbstractBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException.AbstractBusinessException ex, WebRequest request) {
        log.error("BusinessException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .status(ex.getHttpStatus().value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }



    /**
     * 그 외 모든 예외를 처리합니다.
     * 개발환경: 원래 상태코드와 상세 메시지
     * 운영환경: 500 상태코드와 일반 메시지
     * @param ex 발생한 예외
     * @param request 웹 요청
     * @return ErrorResponse를 포함하는 ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        log.error("Exception occurred: {}", ex.getMessage(), ex);

        boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);

        // 1. 예외 자체의 원래 상태 코드 결정
        HttpStatus originalStatus = determineOriginalStatus(ex);

        // 2. 응답 메시지 결정
        String message = isProduction
                ? "서버 내부 오류가 발생했습니다"
                : String.format("%s: %s", ex.getClass().getSimpleName(), ex.getMessage());

        // 3. 실제 HTTP 응답 상태 결정
        HttpStatus responseStatus = isProduction
                ? HttpStatus.INTERNAL_SERVER_ERROR  // 운영: 무조건 500
                : originalStatus;                    // 개발: 원래 상태코드

        // 4. ErrorResponse의 status 필드는 항상 원래 상태코드 (개발자 참고용)
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(message)
                .errorCode(determineErrorCode(originalStatus))
                .status(originalStatus.value())  // 📍 항상 원래 상태코드
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, responseStatus);
    }

    private HttpStatus determineOriginalStatus(Exception ex) {
        // Spring MVC 예외들의 원래 상태코드
        if (ex instanceof HttpMessageNotReadableException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof MethodArgumentTypeMismatchException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof HttpRequestMethodNotSupportedException) return HttpStatus.METHOD_NOT_ALLOWED;
        if (ex instanceof HttpMediaTypeNotSupportedException) return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        if (ex instanceof NoHandlerFoundException) return HttpStatus.NOT_FOUND;
        if (ex instanceof MissingServletRequestParameterException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof ResponseStatusException) {
            return HttpStatus.valueOf(((ResponseStatusException) ex).getStatusCode().value());
        }
        if (ex instanceof java.time.format.DateTimeParseException) return HttpStatus.BAD_REQUEST;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String determineErrorCode(HttpStatus status) {
        return switch (status.series()) {
            case CLIENT_ERROR -> "CLIENT_ERROR_" + status.value();
            case SERVER_ERROR -> "SERVER_ERROR_" + status.value();
            default -> "ERROR_" + status.value();
        };
    }
}