package com.wms.stock.domain.model;

import com.wms.location.domain.exception.LocationException;
import com.wms.stock.domain.exception.StockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Stock 도메인 테스트")
class StockTest {

	@Test
	@DisplayName("재고 수량 정상 추가")
	void testAddQuantitySuccess() {
		// Given
		StockKey key = StockKey.of(1L, 100L); // Mock ID 사용

		Stock stock = Stock.builder()
				.key(key)   // Mock ID 사용
				.quantity(10)
				.build();

		int warehouseCapacity = 100;
		int currentPalletCount = 50;
		int addedQuantity = 20;

		// When
		stock.plusQuantityWithCapacityCheck(warehouseCapacity, currentPalletCount, addedQuantity);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(30); // 10 + 20
	}

	@Test
	@DisplayName("재고 추가 시 창고 용량 초과 예외 발생")
	void testAddQuantityCapacityExceeded() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId); // Mock ID 사용

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(10)
				.build();

		int warehouseCapacity = 60;
		int currentPalletCount = 50;
		int addedQuantity = 20; // 50 + 20 = 70 > 60 (용량 초과)

		// When & Then
		assertThatThrownBy(() -> stock.plusQuantityWithCapacityCheck(warehouseCapacity, currentPalletCount, addedQuantity)).isInstanceOf(LocationException.WarehouseCapacityExceededEx.class)
				.hasMessageContaining("용량 초과");
	}

	@Test
	@DisplayName("재고 추가 시 정확히 용량에 맞는 경우 성공")
	void testAddQuantityExactCapacity() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId); // Mock ID 사용

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(10)
				.build();

		int warehouseCapacity = 70;
		int currentPalletCount = 50;
		int addedQuantity = 20; // 50 + 20 = 70 (정확히 용량에 맞음)

		// When
		stock.plusQuantityWithCapacityCheck(warehouseCapacity, currentPalletCount, addedQuantity);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(30);
	}

	@Test
	@DisplayName("재고 정상 제거")
	void testRemoveQuantitySuccess() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId); // Mock ID 사용

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(50) // 초기 재고
				.build();

		int oldQuantity = 50;
		int removeQuantity = 30;

		// When
		boolean shouldDelete = stock.minusQuantityWithStockCheck(oldQuantity, removeQuantity);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(20); // 50 - 30
		assertThat(shouldDelete).isFalse(); // 0이 아니므로 삭제 불필요
	}

	@Test
	@DisplayName("재고 제거 시 부족 예외 발생")
	void testRemoveQuantityInsufficientStock() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(10)
				.build();

		int oldQuantity = 10;
		int removeQuantity = 20; // 재고보다 많이 제거 시도

		// When & Then
		assertThatThrownBy(() -> stock.minusQuantityWithStockCheck(oldQuantity, removeQuantity)).isInstanceOf(StockException.InsufficientStockEx.class)
				.hasMessageContaining("재고 부족");
	}

	@Test
	@DisplayName("재고 제거 후 수량이 0이 되면 삭제 마킹")
	void testRemoveQuantityToZero() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(30)
				.build();

		int oldQuantity = 30;
		int removeQuantity = 30; // 전체 재고 제거

		// When
		boolean shouldDelete = stock.minusQuantityWithStockCheck(oldQuantity, removeQuantity);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(0);
		assertThat(shouldDelete).isTrue(); // 0이므로 삭제 필요
	}

	@Test
	@DisplayName("재고 수량 직접 업데이트 - 정상")
	void testUpdateQuantitySuccess() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(50)
				.build();

		// When
		boolean shouldDelete = stock.updateQuantityAndCheckDeletion(80);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(80);
		assertThat(shouldDelete).isFalse();
	}

	@Test
	@DisplayName("재고 수량 직접 업데이트 - 0으로 설정 시 삭제 마킹")
	void testUpdateQuantityToZero() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(50)
				.build();

		// When
		boolean shouldDelete = stock.updateQuantityAndCheckDeletion(0);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(0);
		assertThat(shouldDelete).isTrue();
	}

	@Test
	@DisplayName("재고 수량 직접 업데이트 - 음수 설정 시 예외")
	void testUpdateQuantityNegative() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(50)
				.build();

		// When & Then
		assertThatThrownBy(() -> stock.updateQuantityAndCheckDeletion(-10)).isInstanceOf(StockException.CannotNegativeQuantityEx.class)
				.hasMessageContaining("음수");
	}

	@Test
	@DisplayName("Stock 빌더 패턴 검증")
	void testStockBuilderPattern() {
		// Given & When
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(50)
				.build();

		// Then
		assertThat(stock.getKey().getWarehouseId()).isEqualTo(1L);
		assertThat(stock.getKey().getWareId()).isEqualTo(100L);
		assertThat(stock.getQuantity()).isEqualTo(50);
		assertThat(stock.getVersion()).isNull(); // JPA에서 관리
	}

	@Test
	@DisplayName("경계값 테스트 - 최소 재고량")
	void testMinimumQuantity() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(1) // 최소 재고량
				.build();

		// When
		boolean shouldDelete = stock.minusQuantityWithStockCheck(1, 1);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(0);
		assertThat(shouldDelete).isTrue();
	}

	@Test
	@DisplayName("경계값 테스트 - 용량 한계")
	void testCapacityLimit() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(90) // 현재 재고
				.build();

		int warehouseCapacity = 100;
		int currentPalletCount = 90;
		int addedQuantity = 10; // 정확히 용량 한계

		// When
		stock.plusQuantityWithCapacityCheck(warehouseCapacity, currentPalletCount, addedQuantity);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(100);
	}

	@Test
	@DisplayName("복합 시나리오 - 연속적인 재고 변화")
	void testComplexScenario() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(50)
				.build();

		// When & Then
		// 1. 재고 추가
		stock.plusQuantityWithCapacityCheck(200, 100, 30);
		assertThat(stock.getQuantity()).isEqualTo(80);

		// 2. 재고 일부 제거
		boolean shouldDelete1 = stock.minusQuantityWithStockCheck(80, 20);
		assertThat(stock.getQuantity()).isEqualTo(60);
		assertThat(shouldDelete1).isFalse();

		// 3. 재고 전체 제거
		boolean shouldDelete2 = stock.minusQuantityWithStockCheck(60, 60);
		assertThat(stock.getQuantity()).isEqualTo(0);
		assertThat(shouldDelete2).isTrue();
	}

	@Test
	@DisplayName("동일한 값으로 재고 변경 시에도 정상 동작")
	void testSameQuantityUpdate() {
		// Given
		Long warehouseId = 1L;
		Long wareId = 100L;
		StockKey key = StockKey.of(wareId, warehouseId);

		Stock stock = Stock.builder()
				.key(key) // Mock ID 사용
				.quantity(50)
				.build();

		// When
		boolean shouldDelete = stock.updateQuantityAndCheckDeletion(50);

		// Then
		assertThat(stock.getQuantity()).isEqualTo(50);
		assertThat(shouldDelete).isFalse();
	}
}