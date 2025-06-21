package com.wms.location.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.location.domain.exception.LocationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {

	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private LocationType type;

	@Column(name = "capacity")
	private Integer capacity;  // 수용량. WAREHOUSE 타입일 때만 유효;

	@Column(name = "coordinate_x", nullable = false)
	private Integer coordinateX; // 장소 페이지 내 location 요소 장소 x좌표

	@Column(name = "coordinate_y", nullable = false)
	private Integer coordinateY; // 장소 페이지 내 location 요소 장소 x좌표

	public Location(String name, LocationType type, Integer coordinateX, Integer coordinateY) {
		this(name, type, null, coordinateX, coordinateY);
	}

	@Builder
	public Location(String name, LocationType type, Integer capacity, Integer coordinateX, Integer coordinateY) {
		validateLocationData(name, type, capacity);
		this.name = name;
		this.type = type;
		this.capacity = capacity;
		this.coordinateX = coordinateX;
		this.coordinateY = coordinateY;
	}

	public void update(String name, Integer capacity, Integer coordinateX, Integer coordinateY) {
		validateLocationData(name, this.type, capacity);
		this.name = name;
		this.capacity = capacity;
		this.coordinateX = coordinateX;
		this.coordinateY = coordinateY;
	}

	private static void validateLocationData(String name, LocationType type, Integer capacity) {
		if (name == null || name.trim().isEmpty()) {
			throw LocationException.validation(FieldEnum.NAME);
		}

		if (type == null) {
			throw LocationException.validation(FieldEnum.TYPE);
		}

		if (type == LocationType.WAREHOUSE) {
			if (capacity == null || capacity <= 0) {
				throw LocationException.validation("수용량", "창고의 수용량은 0보다 커야 합니다.");
			}
		}
		else {
			if (capacity != null) {
				throw LocationException.validation("입고/출고처는 수용량을 지정할 수 없습니다.");
			}
		}
	}

} 