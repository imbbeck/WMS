package com.wms.stock.dto;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.domain.model.StockKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockDTO {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "재고 생성 요청")
	public static class CreateReq {

		@NotNull(message = "물품 ID는 필수입니다")
		@Schema(description = "물품 ID", example = "1")
		private Long wareId;

		@NotNull(message = "창고 ID는 필수입니다")
		@Schema(description = "창고 ID", example = "1")
		private Long warehouseId;

		@NotNull(message = "수량은 필수입니다")
		@Positive(message = "수량은 자연수이어야 합니다")
		@Schema(description = "수량 (파레트 단위)", example = "10")
		private Integer quantity;

		@Builder
		public CreateReq(Long wareId, Long warehouseId, Integer quantity) {
			this.wareId = wareId;
			this.warehouseId = warehouseId;
			this.quantity = quantity;
		}

		public Stock toEntity() {
			return Stock.builder()
					.key(StockKey.of(wareId, warehouseId))
					.quantity(quantity)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "재고 수정 요청")
	public static class UpdateReq {

		@NotNull(message = "수량은 필수입니다")
		@Positive(message = "수량은 자연수이어야 합니다")
		@Schema(description = "수량 (파레트 단위)", example = "15")
		private Integer quantity;

		@Builder
		public UpdateReq(Integer quantity) {
			this.quantity = quantity;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "재고 관리 응답")
	public static class Res {

		@Schema(description = "재고 ID", example = "1")
		private Long id;

		@Schema(description = "물품 ID", example = "1")
		private Long wareId;

		@Schema(description = "창고 ID", example = "1")
		private Long warehouseId;

		@Schema(description = "수량 (파레트 단위)", example = "10")
		private Integer quantity;

		@Schema(description = "낙관적 락을 위한 버전", example = "1")
		private Long version;

		@Builder
		public Res(Long id, Long wareId, Long warehouseId, Integer quantity, Long version) {
			this.id = id;
			this.wareId = wareId;
			this.warehouseId = warehouseId;
			this.quantity = quantity;
			this.version = version;
		}

		public static Res from(Stock stock) {
			return Res.builder()
					.id(stock.getId())
					.wareId(stock.getKey().getWareId())
					.warehouseId(stock.getKey().getWarehouseId())
					.quantity(stock.getQuantity())
					.version(stock.getVersion())
					.build();
		}
	}
}
