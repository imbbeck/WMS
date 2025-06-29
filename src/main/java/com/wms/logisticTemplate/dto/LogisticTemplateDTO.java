package com.wms.logisticTemplate.dto;

import com.wms.location.domain.model.Location;
import com.wms.logisticTemplate.domain.model.LogisticTemplate;
import com.wms.logisticTemplate.domain.model.LogisticType;
import com.wms.ware.domain.model.Ware;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LogisticTemplateDTO {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "물류 템플릿 생성 요청")
	public static class CreateReq {

		@NotBlank(message = "물류 템플릿 이름은 필수입니다")
		@Schema(description = "물류 템플릿 이름", example = "입고 템플릿 A")
		private String name;

		@NotNull(message = "물류 타입은 필수입니다")
		@Schema(description = "물류 타입", example = "INBOUND", allowableValues = {"INBOUND", "OUTBOUND", "INNER"})
		private LogisticType type;

		@NotNull(message = "물품 ID는 필수입니다")
		@Schema(description = "물품 ID", example = "1")
		private Long wareId;

		@NotNull(message = "출발 장소 ID는 필수입니다")
		@Schema(description = "출발 장소 ID", example = "1")
		private Long fromLocationId;

		@NotNull(message = "도착 장소 ID는 필수입니다")
		@Schema(description = "도착 장소 ID", example = "2")
		private Long toLocationId;

		@NotNull(message = "표준 수량은 필수입니다")
		@Positive(message = "표준 수량은 자연수이어야 합니다")
		@Schema(description = "표준 수량", example = "100")
		private Integer standardQuantity;

		// 테스트 편의성을 위한 빌더 패턴 생성자
		@Builder
		public CreateReq(String name, LogisticType type, Long wareId, Long fromLocationId, Long toLocationId, Integer standardQuantity) {
			this.name = name;
			this.type = type;
			this.wareId = wareId;
			this.fromLocationId = fromLocationId;
			this.toLocationId = toLocationId;
			this.standardQuantity = standardQuantity;
		}

		public LogisticTemplate toEntity(Ware ware, Location fromLocation, Location toLocation) {
			return LogisticTemplate.builder()
					.name(name)
					.type(type)
					.ware(ware)
					.fromLocation(fromLocation)
					.toLocation(toLocation)
					.standardQuantity(standardQuantity)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "물류 템플릿 수정 요청")
	public static class UpdateReq {
		@NotBlank(message = "물류 템플릿 이름은 필수입니다")
		@Schema(description = "물류 템플릿 이름", example = "수정된 입고 템플릿 A")
		private String name;

		@NotNull(message = "물류 타입은 필수입니다")
		@Schema(description = "물류 타입", example = "INBOUND", allowableValues = {"INBOUND", "OUTBOUND", "INNER"})
		private LogisticType type;

		@NotNull(message = "표준 수량은 필수입니다")
		@Positive(message = "표준 수량은 자연수이어야 합니다")
		@Schema(description = "표준 수량", example = "150")
		private Integer standardQuantity;

		@Builder
		public UpdateReq(String name, LogisticType type, Integer standardQuantity) {
			this.name = name;
			this.type = type;
			this.standardQuantity = standardQuantity;
		}

	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "물류 템플릿 응답")
	public static class Res {

		@Schema(description = "물류 템플릿 ID", example = "1")
		private Long id;

		@Schema(description = "물류 템플릿 이름", example = "입고 템플릿 A")
		private String name;

		@Schema(description = "물류 타입", example = "INBOUND")
		private LogisticType type;

		@Schema(description = "물품 ID", example = "1")
		private Long wareId;

		@Schema(description = "물품명", example = "스마트폰")
		private String wareName;

		@Schema(description = "출발 장소 ID", example = "1")
		private Long fromLocationId;

		@Schema(description = "출발 장소명", example = "입고처 A")
		private String fromLocationName;

		@Schema(description = "도착 장소 ID", example = "2")
		private Long toLocationId;

		@Schema(description = "도착 장소명", example = "창고 B")
		private String toLocationName;

		@Schema(description = "표준 수량", example = "100")
		private Integer standardQuantity;


		public Res(LogisticTemplate logisticTemplate) {
			this.id = logisticTemplate.getId();
			this.name = logisticTemplate.getName();
			this.type = logisticTemplate.getType();
			this.wareId = logisticTemplate.getWare().getId();
			this.wareName = logisticTemplate.getWare().getName();
			this.fromLocationId = logisticTemplate.getFromLocation().getId();
			this.fromLocationName = logisticTemplate.getFromLocation().getName();
			this.toLocationId = logisticTemplate.getToLocation().getId();
			this.toLocationName = logisticTemplate.getToLocation().getName();
			this.standardQuantity = logisticTemplate.getStandardQuantity();
		}

		@Builder
		public Res(Long id, String name, LogisticType type, Ware ware, Location fromLocation, Location toLocation, Integer standardQuantity) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.wareId = ware.getId();
			this.wareName = ware.getName();
			this.fromLocationId = fromLocation.getId();
			this.fromLocationName = fromLocation.getName();
			this.toLocationId = toLocation.getId();
			this.toLocationName = toLocation.getName();
			this.standardQuantity = standardQuantity;
		}
	}
}
