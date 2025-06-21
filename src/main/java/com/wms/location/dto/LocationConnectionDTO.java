package com.wms.location.dto;

import com.wms.location.domain.model.LocationConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LocationConnectionDTO {
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Location connection creation request")
	public static class CreateReq {

		@NotNull(message = "첫 번째 장소 ID는 필수입니다")
		@Schema(description = "First location ID", example = "1")
		private Long locationId1;

		@NotNull(message = "두 번째 장소 ID는 필수입니다")
		@Schema(description = "Second location ID", example = "2")
		private Long locationId2;

		@NotNull(message = "이동 시간은 필수입니다")
		@Positive(message = "이동 시간은 0보다 커야 합니다")
		@Schema(description = "Travel time between locations (in minutes)", example = "30")
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
	@Schema(description = "Location connection update request")
	public static class UpdateReq {

		@NotNull(message = "이동 시간은 필수입니다")
		@Positive(message = "이동 시간은 0보다 커야 합니다")
		@Schema(description = "Updated travel time between locations (in minutes)", example = "45")
		private Integer trt;

		// 테스트와 사용 편의를 위해 추가
		@Builder
		public UpdateReq(Integer trt) {
			this.trt = trt;
		}
	}


	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Location connection response")
	public static class Res {

		@Schema(description = "Connection ID", example = "1")
		private Long id;

		@Schema(description = "First location ID", example = "1")
		private Long locationAId;

		@Schema(description = "Second location ID", example = "2")
		private Long locationBId;

		@Schema(description = "Travel time between locations (in minutes)", example = "30")
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

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Connection information for a specific location")
	public static class ConnectionInfo {

		@Schema(description = "Connection ID", example = "1")
		private Long connectionId;

		@Schema(description = "Connected location ID", example = "2")
		private Long connectedLocationId;

		@Schema(description = "Travel time to connected location (in minutes)", example = "30")
		private Integer trt;

		@Builder
		public ConnectionInfo(Long connectionId, Long connectedLocationId, Integer trt) {
			this.connectionId = connectionId;
			this.connectedLocationId = connectedLocationId;
			this.trt = trt;
		}
	}

}
