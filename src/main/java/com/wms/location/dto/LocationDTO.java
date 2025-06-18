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

		// 테스트 편의성을 위한 빌더 패턴 생성자
		@Builder
		public createReq(String name, LocationType type, Integer capacity) {
			this.name = name;
			this.type = type;
			this.capacity = capacity;
		}

		public Location toEntity() {
			return Location.builder()
					.name(name)
					.type(type)
					.capacity(capacity)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class updateReq {

		@NotBlank(message = "장소 이름은 필수입니다")
		private String name;

		@NotNull(message = "장소 타입은 필수입니다")
		private LocationType type;

		private Integer capacity;  // WAREHOUSE 타입일 때만 유효

		@Builder
		public updateReq(String name, LocationType type, Integer capacity) {
			this.name = name;
			this.type = type;
			this.capacity = capacity;
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

		public Res(Location location) {
			this.id = location.getId();
			this.name = location.getName();
			this.type = location.getType();
			this.capacity = location.getCapacity();
		}
	}
}
