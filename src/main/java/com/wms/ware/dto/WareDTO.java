package com.wms.ware.dto;

import com.wms.ware.domain.model.Ware;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class WareDTO {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(name = "WareCreateRequest", description = "물품 생성 요청")
	public static class CreateReq {
		@NotBlank(message = "물품명은 필수입니다")
		@Schema(description = "물품명", example = "스마트폰")
		private String name;

		@NotBlank(message = "물품 타입은 필수입니다")
		@Schema(description = "물품 타입", example = "전자제품")
		private String type;

		@NotNull(message = "파레트당 물품 개수는 필수입니다")
		@Positive(message = "파레트당 물품 개수는 자연수이어야 합니다")
		@Schema(description = "파레트당 물품 개수", example = "100")
		private Integer paletteUnit;

		@Builder
		public CreateReq(String name, String type, Integer paletteUnit) {
			this.name = name;
			this.type = type;
			this.paletteUnit = paletteUnit;
		}

		public Ware toEntity() {
			return Ware.builder()
					.name(name)
					.type(type)
					.paletteUnit(paletteUnit)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(name = "WareUpdateRequest", description = "물품 수정 요청")
	public static class UpdateReq {
		@NotBlank(message = "물품명은 필수입니다")
		@Schema(description = "물품명", example = "스마트폰")
		private String name;

		@NotBlank(message = "물품 타입은 필수입니다")
		@Schema(description = "물품 타입", example = "전자제품")
		private String type;

		@NotNull(message = "파레트당 물품 개수는 필수입니다")
		@Positive(message = "파레트당 물품 개수는 자연수이어야 합니다")
		@Schema(description = "파레트당 물품 개수", example = "100")
		private Integer paletteUnit;

		@Builder
		public UpdateReq(String name, String type, Integer paletteUnit) {
			this.name = name;
			this.type = type;
			this.paletteUnit = paletteUnit;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(name = "WareResponse", description = "물품 응답")
	public static class Res {
		@Schema(description = "물품 ID", example = "1")
		private Long id;
		
		@Schema(description = "물품명", example = "스마트폰")
		private String name;
		
		@Schema(description = "물품 타입", example = "전자제품")
		private String type;
		
		@Schema(description = "파레트당 물품 개수", example = "100")
		private Integer paletteUnit;

		public Res(Ware ware) {
			this.id = ware.getId();
			this.name = ware.getName();
			this.type = ware.getType();
			this.paletteUnit = ware.getPaletteUnit();
		}

		@Builder
		public Res(Long id, String name, String type, Integer paletteUnit) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.paletteUnit = paletteUnit;
		}
	}
}
