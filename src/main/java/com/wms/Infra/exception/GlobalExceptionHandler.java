package com.wms.Infra.exception;

import com.wms.location.domain.exception.LocationException;
import com.wms.stock.domain.exception.StockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation 예외를 400 에러로 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
//        List<String> errors = new ArrayList<>();
//
//        ex.getBindingResult().getFieldErrors().forEach(error -> {
//            errors.add(error.getDefaultMessage());
//        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("입력값이 올바르지 않습니다.")
//                .errors(errors)  // 상세 오류 목록
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)  // 400 반환
                .body(errorResponse);
    }


    @ExceptionHandler(LocationException.NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLocationNotFoundException(LocationException.NotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)  // 404 반환
                .body(errorResponse);
    }


    @ExceptionHandler(StockException.class)
    public ResponseEntity<ErrorResponse> handleStockException(StockException e) {
        log.error("Stock error occurred: ", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(StockException.InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStockException(StockException.InsufficientStockException e) {
        log.error("Insufficient stock error occurred: ", e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(StockException.WarehouseCapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleWarehouseCapacityExceededException(StockException.WarehouseCapacityExceededException e) {
        log.error("Warehouse capacity exceeded error occurred: ", e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Invalid argument error occurred: ", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 내부 오류가 발생했습니다."));
    }
} 