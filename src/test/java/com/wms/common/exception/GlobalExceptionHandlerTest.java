package com.wms.common.exception;

import com.wms.location.domain.exception.LocationException;
import com.wms.movement.domain.exception.MovementException;
import com.wms.stock.domain.exception.StockException;
import com.wms.ware.domain.exception.WareException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("비즈니스 예외 처리 테스트")
    void handleBusinessException() {
        // given
        BusinessException exception = new BusinessException("비즈니스 예외 발생");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("비즈니스 예외 발생");
    }

    @Test
    @DisplayName("재고 예외 처리 테스트")
    void handleStockException() {
        // given
        StockException exception = new StockException.InsufficientStockException(1L, 2L, 10, 5);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleStockException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("재고 부족");
    }

    @Test
    @DisplayName("물류이동 예외 처리 테스트")
    void handleMovementException() {
        // given
        MovementException exception = new MovementException.InvalidStatusException("COMPLETED", "PENDING");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleMovementException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("잘못된 상태 변경");
    }

    @Test
    @DisplayName("위치 예외 처리 테스트")
    void handleLocationException() {
        // given
        LocationException exception = new LocationException.NotFoundException(1L);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleLocationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("존재하지 않는 위치");
    }

    @Test
    @DisplayName("물품 예외 처리 테스트")
    void handleWareException() {
        // given
        WareException exception = new WareException.NotFoundException(1L);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleWareException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("존재하지 않는 물품");
    }

    @Test
    @DisplayName("잘못된 인자 예외 처리 테스트")
    void handleIllegalArgumentException() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 인자");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 인자");
    }

    @Test
    @DisplayName("일반 예외 처리 테스트")
    void handleException() {
        // given
        Exception exception = new Exception("예상치 못한 예외");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("서버 오류가 발생했습니다. 관리자에게 문의해주세요.");
    }
} 