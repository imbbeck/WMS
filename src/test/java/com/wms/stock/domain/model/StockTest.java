package com.wms.stock.domain.model;

import com.wms.location.domain.exception.LocationException;
import com.wms.location.domain.model.Location;
import com.wms.stock.domain.exception.StockException;
import com.wms.ware.domain.model.Ware;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Stock 도메인 테스트")
class StockTest {

	// 기본 테스트용 Location과 Ware 객체 생성 헬퍼
	private Location createWarehouse(int capacity) {
		return Location.builder()
				.name("테스트 창고")
				.type(com.wms.location.domain.model.LocationType.WAREHOUSE)
				.capacity(capacity)
				.coordinateX(0)
				.coordinateY(0)
				.build();
	}

	private Ware createWare() {
		return Ware.builder()
				.name("테스트 물품")
				.type("전자제품")
				.paletteUnit(100)
				.build();
	}

	@Test
	@DisplayName("재고 수량 정상 추가")
	void testAddQuantitySuccess() {
		Location warehouse = createWarehouse(100);
		Ware ware = createWare();

		Stock stock = Stock.builder()
				.warehouseId(warehouse.getId())
				.wareId(ware.getId())
				.quantity(10)
				.build();

		int currentPalletCount = 50;
		int addedQuantity = 20;

		stock.plusQuantityWithCapacityCheck(warehouse.getCapacity(), currentPalletCount, addedQuantity);

		assertThat(stock.getQuantity()).isEqualTo(30);
	}

	@Test
	@DisplayName("재고 추가 시 창고 용량 초과 예외 발생")
	void testAddQuantityCapacityExceeded() {
		Location warehouse = createWarehouse(60);
		Ware ware = createWare();

		Stock stock = Stock.builder()
				.warehouseId(warehouse.getId())
				.wareId(ware.getId())
				.quantity(10)
				.build();

		int currentPalletCount = 50;
		int addedQuantity = 20;

		assertThatThrownBy(() -> {
			stock.plusQuantityWithCapacityCheck(warehouse.getCapacity(), currentPalletCount, addedQuantity);
		}).isInstanceOf(LocationException.WarehouseCapacityExceededEx.class)
				.hasMessageContaining("용량 초과");
	}

	@Test
	@DisplayName("재고 정상 제거")
	void testRemoveQuantitySuccess() {
		Location warehouse = createWarehouse(100);
		Ware ware = createWare();

		Stock stock = Stock.builder()
				.warehouseId(warehouse.getId())
				.wareId(ware.getId())
				.quantity(10)
				.build();

		int currentStock = 50;
		int removeQuantity = 30;

		stock.minusQuantityWithStockCheck(currentStock, removeQuantity);

		assertThat(stock.getQuantity()).isEqualTo(20);
	}

	@Test
	@DisplayName("재고 제거 시 부족 예외 발생")
	void testRemoveQuantityInsufficientStock() {
		Location warehouse = createWarehouse(100);
		Ware ware = createWare();

		Stock stock = Stock.builder()
				.warehouseId(warehouse.getId())
				.wareId(ware.getId())
				.quantity(10)
				.build();

		int currentStock = 10;
		int removeQuantity = 20;

		assertThatThrownBy(() -> {
			stock.minusQuantityWithStockCheck(currentStock, removeQuantity);
		}).isInstanceOf(StockException.InsufficientStockEx.class)
				.hasMessageContaining("재고 부족");
	}
}
