package com.wms.stock.domain.model;

import com.wms.stock.domain.exception.StockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    @DisplayName("재고 생성 테스트")
    void createStock() {
        // given
        Long locationId = 1L;
        Long wareId = 1L;
        int quantity = 10;

        // when
        Stock stock = Stock.create(locationId, wareId, quantity);

        // then
        assertThat(stock.getLocationId()).isEqualTo(locationId);
        assertThat(stock.getWareId()).isEqualTo(wareId);
        assertThat(stock.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("재고 수량 증가 테스트")
    void increaseQuantity() {
        // given
        Stock stock = Stock.create(1L, 1L, 10);
        int amount = 5;

        // when
        stock.increaseQuantity(amount);

        // then
        assertThat(stock.getQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("재고 수량 감소 테스트")
    void decreaseQuantity() {
        // given
        Stock stock = Stock.create(1L, 1L, 10);
        int amount = 5;

        // when
        stock.decreaseQuantity(amount);

        // then
        assertThat(stock.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고 수량이 부족할 때 감소 시도 시 예외 발생")
    void decreaseQuantityInsufficient() {
        // given
        Stock stock = Stock.create(1L, 1L, 10);
        int amount = 15;

        // when & then
        assertThatThrownBy(() -> stock.decreaseQuantity(amount))
            .isInstanceOf(StockException.InsufficientStockException.class)
            .hasMessageContaining("재고 부족");
    }

    @Test
    @DisplayName("음수 수량으로 재고 생성 시도 시 예외 발생")
    void createStockWithNegativeQuantity() {
        // given
        Long locationId = 1L;
        Long wareId = 1L;
        int quantity = -10;

        // when & then
        assertThatThrownBy(() -> Stock.create(locationId, wareId, quantity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("수량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("음수 수량으로 재고 증가 시도 시 예외 발생")
    void increaseQuantityWithNegative() {
        // given
        Stock stock = Stock.create(1L, 1L, 10);
        int amount = -5;

        // when & then
        assertThatThrownBy(() -> stock.increaseQuantity(amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("증가 수량은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("음수 수량으로 재고 감소 시도 시 예외 발생")
    void decreaseQuantityWithNegative() {
        // given
        Stock stock = Stock.create(1L, 1L, 10);
        int amount = -5;

        // when & then
        assertThatThrownBy(() -> stock.decreaseQuantity(amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("감소 수량은 0보다 커야 합니다");
    }
} 