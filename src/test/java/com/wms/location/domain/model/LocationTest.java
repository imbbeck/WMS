package com.wms.location.domain.model;

import com.wms.location.domain.exception.LocationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("장소 도메인 테스트")
class LocationTest {

	@Test
	@DisplayName("장소명이 null이면 예외가 발생한다")
	void testLocationNameNull() {
		assertThatThrownBy(() -> {
			Location.builder()
					.name(null)
					.type(LocationType.WAREHOUSE)
					.capacity(1000)
					.coordinateX(100)
					.coordinateY(100)
					.build();
		}).isInstanceOf(LocationException.ValidationEx.class)
				.hasMessage("이름 값이 유효하지 않습니다.");
	}

	@Test
	@DisplayName("장소 타입이 null이면 예외가 발생한다")
	void testLocationTypeNull() {
		assertThatThrownBy(() -> {
			Location.builder()
					.name("테스트 장소")
					.type(null)
					.capacity(1000)
					.coordinateX(100)
					.coordinateY(100)
					.build();
		}).isInstanceOf(LocationException.ValidationEx.class)
				.hasMessage("타입 값이 유효하지 않습니다.");
	}

	@Test
	@DisplayName("창고 타입에서 수용량이 0이하면 예외가 발생한다")
	void testWarehouseCapacityInvalid() {
		assertThatThrownBy(() -> {
			Location.builder()
					.name("테스트 창고")
					.type(LocationType.WAREHOUSE)
					.capacity(0)
					.coordinateX(100)
					.coordinateY(100)
					.build();
		}).isInstanceOf(LocationException.ValidationEx.class)
				.hasMessage("수용량 값이 유효하지 않습니다. 창고의 수용량은 0보다 커야 합니다.");
	}

	@Test
	@DisplayName("입고처에서 수용량을 지정하면 예외가 발생한다")
	void testInboundLocationWithCapacity() {
		assertThatThrownBy(() -> {
			Location.builder()
					.name("테스트 입고처")
					.type(LocationType.INBOUND)
					.capacity(1000)
					.coordinateX(100)
					.coordinateY(100)
					.build();
		}).isInstanceOf(LocationException.ValidationEx.class)
				.hasMessage("입고/출고처는 수용량을 지정할 수 없습니다.");
	}

	@Test
	@DisplayName("유효한 장소 정보로 장소를 생성할 수 있다")
	void testValidLocationCreation() {
		Location location = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(1000)
				.coordinateX(100)
				.coordinateY(100)
				.build();

		assertThat(location.getName()).isEqualTo("테스트 창고");
		assertThat(location.getType()).isEqualTo(LocationType.WAREHOUSE);
		assertThat(location.getCapacity()).isEqualTo(1000);
		assertThat(location.getCoordinateX()).isEqualTo(100);
		assertThat(location.getCoordinateY()).isEqualTo(100);
	}

	@Test
	@DisplayName("장소 정보를 업데이트할 수 있다")
	void testLocationUpdate() {
		Location location = Location.builder()
				.name("테스트 창고")
				.type(LocationType.WAREHOUSE)
				.capacity(1000)
				.coordinateX(100)
				.coordinateY(100)
				.build();

		location.update("업데이트된 창고", 2000, 200, 200);

		assertThat(location.getName()).isEqualTo("업데이트된 창고");
		assertThat(location.getCapacity()).isEqualTo(2000);
		assertThat(location.getCoordinateX()).isEqualTo(200);
		assertThat(location.getCoordinateY()).isEqualTo(200);
	}
}