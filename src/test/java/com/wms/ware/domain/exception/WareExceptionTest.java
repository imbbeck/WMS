package com.wms.ware.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WareExceptionTest {

    @Test
    @DisplayName("NotFoundException 메시지 테스트")
    void notFoundExceptionMessage() {
        // given
        Long wareId = 1L;

        // when
        WareException.NotFoundException exception = 
            new WareException.NotFoundException(wareId);

        // then
        assertThat(exception.getMessage())
            .contains("존재하지 않는 물품")
            .contains(wareId.toString());
    }

    @Test
    @DisplayName("InvalidQuantityException 메시지 테스트")
    void invalidQuantityExceptionMessage() {
        // given
        Long wareId = 1L;
        int quantity = -10;

        // when
        WareException.InvalidQuantityException exception = 
            new WareException.InvalidQuantityException(wareId, quantity);

        // then
        assertThat(exception.getMessage())
            .contains("잘못된 물품 수량")
            .contains(wareId.toString())
            .contains(String.valueOf(quantity));
    }
} 