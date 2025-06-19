package com.wms.location.dto;

import com.wms.location.domain.model.Location;
import com.wms.location.domain.model.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LocationDTO {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class createReq {

		@NotBlank(message = "장소 이름은 필수입니다")
		private String name;

		@NotNull(message = "장소 타입은 필수입니다")
		private LocationType type;

		private Integer capacity;  // WAREHOUSE 타입일 때만 유효

		@NotNull(message = "x좌표는 필수입니다")
		private Integer coordinateX;

		@NotNull(message = "y좌표는 필수입니다")
		private Integer coordinateY;

		// 테스트 편의성을 위한 빌더 패턴 생성자
		@Builder
		public createReq(String name, LocationType type, Integer capacity, Integer coordinateX, Integer coordinateY) {
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
	public static class updateReq {

		@NotBlank(message = "장소 이름은 필수입니다")
		private String name;

		private Integer capacity;  // WAREHOUSE 타입일 때만 유효

		@NotNull(message = "x좌표는 필수입니다")
		private Integer coordinateX;

		@NotNull(message = "ㅛ좌표는 필수입니다")
		private Integer coordinateY;

		@Builder
		public updateReq(String name, Integer capacity, Integer coordinateX, Integer coordinateY) {
			this.name = name;
			this.capacity = capacity;
			this.coordinateX = coordinateX;
			this.coordinateY = coordinateY;
		}

	}

	// 연결 정보 없는 간단한 응답용
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Res {

		private Long id;
		private String name;
		private LocationType type;
		private Integer capacity;
		private Integer coordinateX;
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
