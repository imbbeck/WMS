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
	@Schema(name = "LocationConnectionCreateRequest", description = "장소 연결 생성 요청")
	public static class CreateReq {

		@NotNull(message = "첫 번째 장소 ID는 필수입니다")
		@Schema(description = "첫 번째 장소 ID", example = "1")
		private Long locationId1;

		@NotNull(message = "두 번째 장소 ID는 필수입니다")
		@Schema(description = "두 번째 장소 ID", example = "2")
		private Long locationId2;

		@NotNull(message = "이동 시간은 필수입니다")
		@Positive(message = "이동 시간은 0보다 커야 합니다")
		@Schema(description = "장소 간 이동 시간(분)", example = "30")
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
	@Schema(name = "LocationConnectionUpdateRequest", description = "장소 연결 수정 요청")
	public static class UpdateReq {

		@NotNull(message = "이동 시간은 필수입니다")
		@Positive(message = "이동 시간은 0보다 커야 합니다")
		@Schema(description = "수정된 장소 간 이동 시간(분)", example = "45")
		private Integer trt;

		// 테스트와 사용 편의를 위해 추가
		@Builder
		public UpdateReq(Integer trt) {
			this.trt = trt;
		}
	}


	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(name = "LocationConnectionResponse", description = "장소 연결 응답")
	public static class Res {

		@Schema(description = "연결 ID", example = "1")
		private Long id;

		@Schema(description = "첫 번째 장소 ID", example = "1")
		private Long locationAId;

		@Schema(description = "두 번째 장소 ID", example = "2")
		private Long locationBId;

		@Schema(description = "장소 간 이동 시간(분)", example = "30")
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
	@Schema(name = "ConnectionInfo", description = "특정 장소의 연결 정보")
	public static class ConnectionInfo {

		@Schema(description = "연결 ID", example = "1")
		private Long connectionId;

		@Schema(description = "연결된 장소 ID", example = "2")
		private Long connectedLocationId;

		@Schema(description = "연결된 장소까지의 이동 시간(분)", example = "30")
		private Integer trt;

		@Builder
		public ConnectionInfo(Long connectionId, Long connectedLocationId, Integer trt) {
			this.connectionId = connectionId;
			this.connectedLocationId = connectedLocationId;
			this.trt = trt;
		}
	}

}
