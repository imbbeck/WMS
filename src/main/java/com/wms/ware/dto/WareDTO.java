package com.wms.ware.dto;

import com.wms.ware.domain.model.Ware;
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
	public static class CreateReq {
		@NotBlank(message = "물품 이름은 필수입니다")
		private String name;

		@NotBlank(message = "물품 타입은 필수입니다")
		private String type;

		@NotNull(message = "파레트 당 물품 개수는 필수입니다")
		@Positive(message = "파레트 당 물품 개수는 0보다 커야 합니다")
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
	public static class UpdateReq {
		@NotBlank(message = "물품 이름은 필수입니다")
		private String name;

		@NotBlank(message = "물품 타입은 필수입니다")
		private String type;

		@NotNull(message = "파레트 당 물품 개수는 필수입니다")
		@Positive(message = "파레트 당 물품 개수는 0보다 커야 합니다")
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
	public static class Res {
		private Long id;
		private String name;
		private String type;
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
