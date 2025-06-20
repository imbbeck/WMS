package com.wms.location.dto;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LocationDTO {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Location creation request")
	public static class CreateReq {

		@NotBlank(message = "장소 이름은 필수입니다")
		@Schema(description = "Location name", example = "Warehouse A")
		private String name;

		@NotNull(message = "장소 타입은 필수입니다")
		@Schema(description = "Location type", example = "INBOUND, OUTBOUND, WAREHOUSE")
		private LocationType type;

		@Schema(description = "Storage capacity (only valid for WAREHOUSE type)", example = "1000")
		private Integer capacity;  // WAREHOUSE 타입일 때만 유효

		@NotNull(message = "x좌표는 필수입니다")
		@Schema(description = "X coordinate", example = "10")
		private Integer coordinateX;

		@NotNull(message = "y좌표는 필수입니다")
		@Schema(description = "Y coordinate", example = "20")
		private Integer coordinateY;

		// 테스트 편의성을 위한 빌더 패턴 생성자
		@Builder
		public CreateReq(String name, LocationType type, Integer capacity, Integer coordinateX, Integer coordinateY) {
			this.name = name;
			this.type = type;
			this.capacity = capacity;
			this.coordinateX = coordinateX;
			this.coordinateY = coordinateY;
		}

		public Location toEntity() {
			return Location.builder()
					.name(name)
					.type(type)
					.capacity(capacity)
					.coordinateX(coordinateX)
					.coordinateY(coordinateY)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Location update request")
	public static class UpdateReq {
		@NotBlank(message = "장소 이름은 필수입니다")
		@Schema(description = "Location name", example = "Updated Warehouse A")
		private String name;

		@Schema(description = "Storage capacity (only valid for WAREHOUSE type)", example = "1500")
		private Integer capacity;  // WAREHOUSE 타입일 때만 유효

		@NotNull(message = "x좌표는 필수입니다")
		@Schema(description = "X coordinate", example = "15")
		private Integer coordinateX;

		@NotNull(message = "y좌표는 필수입니다")
		@Schema(description = "Y coordinate", example = "25")
		private Integer coordinateY;

		@Builder
		public UpdateReq(String name, Integer capacity, Integer coordinateX, Integer coordinateY) {
			this.name = name;
			this.capacity = capacity;
			this.coordinateX = coordinateX;
			this.coordinateY = coordinateY;
		}

	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Location response")
	public static class Res {

		@Schema(description = "Location ID", example = "1")
		private Long id;

		@Schema(description = "Location name", example = "Warehouse A")
		private String name;

		@Schema(description = "Location type", example = "WAREHOUSE")
		private LocationType type;

		@Schema(description = "Storage capacity", example = "1000")
		private Integer capacity;

		@Schema(description = "X coordinate", example = "10")
		private Integer coordinateX;

		@Schema(description = "Y coordinate", example = "20")
		private Integer coordinateY;

		public Res(Location location) {
			this.id = location.getId();
			this.name = location.getName();
			this.type = location.getType();
			this.capacity = location.getCapacity();
			this.coordinateX = location.getCoordinateX();
			this.coordinateY = location.getCoordinateY();
		}

		@Builder
		public Res(Long id, String name, LocationType type, Integer capacity, Integer coordinateX, Integer coordinateY) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.capacity = capacity;
			this.coordinateX = coordinateX;
			this.coordinateY = coordinateY;
		}
	}
}
