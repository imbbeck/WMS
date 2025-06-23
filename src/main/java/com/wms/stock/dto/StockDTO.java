package com.wms.stock.dto;

import com.wms.stock.domain.model.Stock;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
		@PositiveOrZero(message = "수량은 0 이상이어야 합니다")
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
		@PositiveOrZero(message = "수량은 0 이상이어야 합니다")
		@Schema(description = "Quantity (palette unit)", example = "15")
		private Integer quantity;

		@Builder
		public UpdateReq(Integer quantity) {
			this.quantity = quantity;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Stock response")
	public static class Res {

		@Schema(description = "Stock ID", example = "1")
		private Long id;

		@Schema(description = "Ware ID", example = "1")
		private Long wareId;

		@Schema(description = "Warehouse ID", example = "1")
		private Long warehouseId;

		@Schema(description = "Quantity (palette unit)", example = "10")
		private Integer quantity;

		@Builder
		public Res(Long id, Long wareId, Long warehouseId, Integer quantity) {
			this.id = id;
			this.wareId = wareId;
			this.warehouseId = warehouseId;
			this.quantity = quantity;
		}

		public static Res from(Stock stock) {
			return Res.builder()
					.id(stock.getId())
					.wareId(stock.getWareId())
					.warehouseId(stock.getWarehouseId())
					.quantity(stock.getQuantity())
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Stock search criteria")
	public static class SearchReq {

		@Schema(description = "Ware ID filter", example = "1")
		private Long wareId;

		@Schema(description = "Warehouse ID filter", example = "1")
		private Long warehouseId;

		@Schema(description = "Minimum quantity filter", example = "5")
		private Integer minQuantity;

		@Schema(description = "Maximum quantity filter", example = "100")
		private Integer maxQuantity;

		@Builder
		public SearchReq(Long wareId, Long warehouseId, Integer minQuantity, Integer maxQuantity) {
			this.wareId = wareId;
			this.warehouseId = warehouseId;
			this.minQuantity = minQuantity;
			this.maxQuantity = maxQuantity;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Warehouse stock summary")
	public static class WarehouseSummaryRes {

		@Schema(description = "Warehouse ID", example = "1")
		private Long warehouseId;

		@Schema(description = "Warehouse name", example = "중앙창고")
		private String warehouseName;

		@Schema(description = "Total palette count", example = "50")
		private Integer totalPaletteCount;

		@Schema(description = "Warehouse capacity", example = "100")
		private Integer capacity;

		@Schema(description = "Utilization rate", example = "50.0")
		private Double utilizationRate;

		@Schema(description = "Number of different ware types", example = "5")
		private Integer wareTypeCount;

		@Builder
		public WarehouseSummaryRes(Long warehouseId, String warehouseName,
				Integer totalPaletteCount, Integer capacity,
				Double utilizationRate, Integer wareTypeCount) {
			this.warehouseId = warehouseId;
			this.warehouseName = warehouseName;
			this.totalPaletteCount = totalPaletteCount;
			this.capacity = capacity;
			this.utilizationRate = utilizationRate;
			this.wareTypeCount = wareTypeCount;
		}
	}
}