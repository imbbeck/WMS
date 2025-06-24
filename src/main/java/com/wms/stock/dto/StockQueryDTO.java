package com.wms.stock.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockQueryDTO {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Stock search criteria")
	public static class SearchReq {

		@Schema(description = "Ware ID filter", example = "1")
		private Long wareId;

		@Schema(description = "Warehouse ID filter", example = "1")
		private Long warehouseId;

		@Builder
		public SearchReq(Long wareId, Long warehouseId) {
			this.wareId = wareId;
			this.warehouseId = warehouseId;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "StockView response")
	public static class Res {

		@Schema(description = "Ware ID", example = "1")
		private Long wareId;

		@Schema(description = "Ware name", example = "물품 A")
		private String wareName;

		@Schema(description = "Warehouse ID", example = "1")
		private Long warehouseId;

		@Schema(description = "Warehouse name", example = "중앙창고")
		private String warehouseName;

		@Schema(description = "Quantity (palette unit)", example = "10")
		private Integer quantity;

		@Builder
		public Res(Long wareId, String wareName, Long warehouseId, String warehouseName, Integer quantity) {
			this.wareId = wareId;
			this.wareName = wareName;
			this.warehouseId = warehouseId;
			this.warehouseName = warehouseName;
			this.quantity = quantity;
		}

		public static Res from(Long wareId, String wareName, Long warehouseId, String warehouseName, Integer quantitye) { //wareName, warehouseName은 외부에서 id-name pair 캐시를 이용해 주입
			return Res.builder()
					.wareId(wareId)
					.wareName(wareName)
					.warehouseId(warehouseId)
					.warehouseName(warehouseName)
					.quantity(quantitye)
					.build();
		}
	}


	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Ware-wise stock aggregation")
	public static class WareAggregationRes {

		@Schema(description = "Ware ID", example = "1")
		private Long wareId;

		@Schema(description = "Ware name", example = "물품 A")
		private String wareName;

		@Schema(description = "Total quantity across all warehouses", example = "150")
		private Integer totalQuantity;

		@Schema(description = "Number of warehouses storing this ware", example = "3")
		private Integer warehouseCount;

		@Schema(description = "Child Warehouse List", example = "[{'warehouseId':1,'warehouseName':'중앙창고','quantity':50}, {'warehouseId':2,'warehouseName':'지점창고','quantity':30}]")
		private List<WarehouseUnit> stockList;

		@Builder
		public WareAggregationRes(
				Long wareId, String wareName,
				Integer totalQuantity, Integer warehouseCount,
				List<WarehouseUnit> stockList
		) {
			this.wareId = wareId;
			this.wareName = wareName;
			this.totalQuantity = totalQuantity;
			this.warehouseCount = warehouseCount;
			this.stockList = stockList;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Warehouse-wise stock aggregation")
	public static class WarehouseAggregationRes {

		@Schema(description = "Warehouse ID", example = "1")
		private Long warehouseId;

		@Schema(description = "Warehouse name", example = "중앙창고")
		private String warehouseName;

		@Schema(description = "Total quantity in this warehouse", example = "150")
		private Integer totalQuantity;

		@Schema(description = "Warehouse capacity", example = "100")
		private Integer capacity;

		@Schema(description = "Available capacity", example = "25")
		private Integer availableCapacity;

		@Schema(description = "Number of different ware types", example = "5")
		private Integer wareTypeCount;

		@Schema(description = "Child Ware List", example = "[{'wareId':1,'wareName':'물품 A','quantity':50}, {'wareId':2,'wareName':'물품 B','quantity':30}]")
		private List<WareUnit> stockList;

		@Builder
		public WarehouseAggregationRes(
				Long warehouseId, String warehouseName,
				Integer totalQuantity, Integer capacity,
				Integer wareTypeCount, List<WareUnit> stockList
		) {
			this.warehouseId = warehouseId;
			this.warehouseName = warehouseName;
			this.totalQuantity = totalQuantity;
			this.capacity = capacity;
			this.availableCapacity = capacity - totalQuantity; // 계산된 필드
			this.wareTypeCount = wareTypeCount;
			this.stockList = stockList;
		}
	}


	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Warehouse stock information")
	public static class WarehouseUnit {

		@Schema(description = "Warehouse ID", example = "1")
		private Long warehouseId;

		@Schema(description = "Warehouse name", example = "중앙창고")
		private String warehouseName;

		@Schema(description = "Quantity", example = "50")
		private Integer quantity;


		@Builder
		public WarehouseUnit(Long warehouseId, String warehouseName, Integer quantity) {
			this.warehouseId = warehouseId;
			this.warehouseName = warehouseName;
			this.quantity = quantity;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "Ware stock information")
	public static class WareUnit {

		@Schema(description = "Ware ID", example = "1")
		private Long wareId;

		@Schema(description = "Ware name", example = "물품 A")
		private String wareName;

		@Schema(description = "Quantity", example = "50")
		private Integer quantity;

		@Builder
		public WareUnit(Long wareId, String wareName, Integer quantity) {
			this.wareId = wareId;
			this.wareName = wareName;
			this.quantity = quantity;
		}
	}
}