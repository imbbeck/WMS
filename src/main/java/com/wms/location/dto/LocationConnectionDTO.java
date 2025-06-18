package com.wms.location.dto;

import java.time.LocalDateTime;

import com.wms.location.domain.model.LocationConnection;
import com.wms.location.domain.repository.LocationCacheManager;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LocationConnectionDTO {
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class CreateReq {

		@NotNull(message = "첫 번째 위치 ID는 필수입니다")
		private Long locationId1;

		@NotNull(message = "두 번째 위치 ID는 필수입니다")
		private Long locationId2;

		@NotNull(message = "이동 시간은 필수입니다")
		@Positive(message = "이동 시간은 0보다 커야 합니다")
		private Integer trt;

		@Builder
		public CreateReq(Long locationId1, Long locationId2, Integer trt) {
			this.locationId1 = locationId1;
			this.locationId2 = locationId2;
			this.trt = trt;
		}

		public LocationConnection toEntity() {
			return LocationConnection.builder()
					.locationId1(locationId1)
					.locationId2(locationId2)
					.trt(trt)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UpdateReq {

		@NotNull(message = "이동 시간은 필수입니다")
		@Positive(message = "이동 시간은 0보다 커야 합니다")
		private Integer trt;

		// 테스트와 사용 편의를 위해 추가
		@Builder
		public UpdateReq(Integer trt) {
			this.trt = trt;
		}
	}


	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Res {

		private Long id;
		private Long locationAId;
		private Long locationBId;
		private Integer trt;

		public static Res from(LocationConnection connection) {
			return Res.builder()
					.id(connection.getId())
					.locationAId(connection.getLocationAId())
					.locationBId(connection.getLocationBId())
					.trt(connection.getTrt())
					.build();
		}

		@Builder
		public Res(Long id, Long locationAId, Long locationBId, Integer trt) {
			this.id = id;
			this.locationAId = locationAId;
			this.locationBId = locationBId;
			this.trt = trt;
		}
	}

	// 특정 Location의 연결 정보 응답용
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class ConnectionInfo {

		private Long connectionId;
		private Long connectedLocationId;
		private Integer trt;

		@Builder
		public ConnectionInfo(Long connectionId, Long connectedLocationId, Integer trt) {
			this.connectionId = connectionId;
			this.connectedLocationId = connectedLocationId;
			this.trt = trt;
		}
	}

}
