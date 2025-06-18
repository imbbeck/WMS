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
		private String locationAName;
		private Long locationBId;
		private String locationBName;
		private Integer trt;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;

		// Location 이름 정보 포함 생성자
		@Builder
		public Res(LocationConnection connection, LocationCacheManager locationCache) {
			this.id = connection.getId();
			this.locationAId = connection.getLocationAId();
			this.locationAName = locationCache.getName(this.locationAId);
			this.locationBId = connection.getLocationBId();
			this.locationBName = locationCache.getName(this.locationBId);
			this.trt = connection.getTrt();
			this.createdAt = connection.getCreatedAt();
			this.updatedAt = connection.getUpdatedAt();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class SimpleRes {

		private Long id;
		private Long locationAId;
		private Long locationBId;
		private Integer trt;

		public SimpleRes(LocationConnection connection) {
			this.id = connection.getId();
			this.locationAId = connection.getLocationAId();
			this.locationBId = connection.getLocationBId();
			this.trt = connection.getTrt();
		}
	}

	// 특정 Location의 연결 정보 응답용
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class ConnectionInfo {

		private Long connectionId;
		private Long connectedLocationId;
		private String connectedLocationName;
		private Integer trt;

		@Builder
		public ConnectionInfo(Long connectionId, Long connectedLocationId, String connectedLocationName, Integer trt) {
			this.connectionId = connectionId;
			this.connectedLocationId = connectedLocationId;
			this.connectedLocationName = connectedLocationName;
			this.trt = trt;
		}
	}

}
