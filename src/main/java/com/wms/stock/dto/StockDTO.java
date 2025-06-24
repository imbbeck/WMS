package com.wms.stock.dto;

import com.wms.stock.domain.model.Stock;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockDTO {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Stock creation request")
	public static class CreateReq {

		@NotNull(message = "물품 ID는 필수입니다")
		@Schema(description = "Ware ID", example = "1")
		private Long wareId;

		@NotNull(message = "창고 ID는 필수입니다")
		@Schema(description = "Warehouse ID", example = "1")
		private Long warehouseId;

		@NotNull(message = "수량은 필수입니다")
		@Positive(message = "수량은 자연수이어야 합니다")
		@Schema(description = "Quantity (palette unit)", example = "10")
		private Integer quantity;

		@Builder
		public CreateReq(Long wareId, Long warehouseId, Integer quantity) {
			this.wareId = wareId;
			this.warehouseId = warehouseId;
			this.quantity = quantity;
		}

		public Stock toEntity() {
			return Stock.builder()
					.wareId(wareId)
					.warehouseId(warehouseId)
					.quantity(quantity)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Stock update request")
	public static class UpdateReq {

		@NotNull(message = "수량은 필수입니다")
		@Positive(message = "수량은 자연수이어야 합니다")
		@Schema(description = "Quantity (palette unit)", example = "15")
		private Integer quantity;

		@Builder
		public UpdateReq(Integer quantity) {
			this.quantity = quantity;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Stock ctrl response")
	public static class Res {

		@Schema(description = "Stock ID", example = "1")
		private Long id;

		@Schema(description = "Ware ID", example = "1")
		private Long wareId;

		@Schema(description = "Warehouse ID", example = "1")
		private Long warehouseId;

		@Schema(description = "Quantity (palette unit)", example = "10")
		private Integer quantity;

		@Schema(description = "Version for optimistic locking", example = "1")
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
					.wareId(stock.getWareId())
					.warehouseId(stock.getWarehouseId())
					.quantity(stock.getQuantity())
					.version(stock.getVersion())
					.build();
		}
	}
}