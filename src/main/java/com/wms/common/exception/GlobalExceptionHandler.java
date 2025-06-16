package com.wms.common.exception;

import com.wms.stock.domain.exception.StockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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