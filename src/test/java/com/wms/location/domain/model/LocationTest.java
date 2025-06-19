package com.wms.location.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationTest {

	@Test
	@DisplayName("창고 타입의 위치 생성 시 용량이 없으면 예외가 발생한다.")
	void createWarehouse_WithNullCapacity_ThrowsException() {
		// when & then
		assertThatThrownBy(() -> {
			Location.builder()
					.name("창고 A")
					.type(LocationType.WAREHOUSE)
					.capacity(null) // 용량 누락
					.coordinateX(100)
					.coordinateY(100)
					.build();
		}).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("창고의 용량은 0보다 커야 합니다.");
	}

	@Test
	@DisplayName("창고가 아닌 타입의 위치에 용량을 지정하면 예외가 발생한다.")
	void createNonWarehouse_WithCapacity_ThrowsException() {
		// when & then
		assertThatThrownBy(() -> {
			Location.builder()
					.name("입고존")
					.type(LocationType.INBOUND)
					.capacity(100) // 불필요한 용량 지정
					.coordinateX(100)
					.coordinateY(100)
					.build();
		}).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("입고/출고처는 용량을 지정할 수 없습니다.");
	}
}