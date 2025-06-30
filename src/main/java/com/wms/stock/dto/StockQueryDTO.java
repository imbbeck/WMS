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
	@Schema(name = "StockSearchRequest", description = "재고 검색 조건")
	public static class SearchReq {

		@Schema(description = "물품 ID 필터", example = "1")
		private Long wareId;

		@Schema(description = "창고 ID 필터", example = "1")
		private Long warehouseId;

		@Builder
		public SearchReq(Long wareId, Long warehouseId) {
			this.wareId = wareId;
			this.warehouseId = warehouseId;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(name = "StockQueryResponse", description = "재고 조회 응답")
	public static class Res {

		@Schema(description = "물품 ID", example = "1")
		private Long wareId;

		@Schema(description = "물품명", example = "스마트폰")
		private String wareName;

		@Schema(description = "창고 ID", example = "1")
		private Long warehouseId;

		@Schema(description = "창고명", example = "중앙창고")
		private String warehouseName;

		@Schema(description = "수량 (파레트 단위)", example = "10")
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
	@Schema(name = "WareAggregationResponse", description = "물품별 재고 집계")
	public static class WareAggregationRes {

		@Schema(description = "물품 ID", example = "1")
		private Long wareId;

		@Schema(description = "물품명", example = "스마트폰")
		private String wareName;

		@Schema(description = "모든 창고의 총 수량", example = "150")
		private Integer totalQuantity;

		@Schema(description = "이 물품을 보관하는 창고 수", example = "3")
		private Integer warehouseCount;

		@Schema(description = "하위 창고 목록", example = "[{'warehouseId':1,'warehouseName':'중앙창고','quantity':50}, {'warehouseId':2,'warehouseName':'지점창고','quantity':30}]")
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
	@Schema(name = "WarehouseAggregationResponse", description = "창고별 재고 집계")
	public static class WarehouseAggregationRes {

		@Schema(description = "창고 ID", example = "1")
		private Long warehouseId;

		@Schema(description = "창고명", example = "중앙창고")
		private String warehouseName;

		@Schema(description = "이 창고의 총 수량", example = "150")
		private Integer totalQuantity;

		@Schema(description = "창고 용량", example = "200")
		private Integer capacity;

		@Schema(description = "사용 가능한 용량", example = "50")
		private Integer availableCapacity;

		@Schema(description = "서로 다른 물품 타입 수", example = "5")
		private Integer wareTypeCount;

		@Schema(description = "하위 물품 목록", example = "[{'wareId':1,'wareName':'스마트폰','quantity':50}, {'wareId':2,'wareName':'태블릿','quantity':30}]")
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
	@Schema(name = "WarehouseUnit", description = "창고 재고 정보")
	public static class WarehouseUnit {

		@Schema(description = "창고 ID", example = "1")
		private Long warehouseId;

		@Schema(description = "창고명", example = "중앙창고")
		private String warehouseName;

		@Schema(description = "수량", example = "50")
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
	@Schema(name = "WareUnit", description = "물품 재고 정보")
	public static class WareUnit {

		@Schema(description = "물품 ID", example = "1")
		private Long wareId;

		@Schema(description = "물품명", example = "스마트폰")
		private String wareName;

		@Schema(description = "수량", example = "50")
		private Integer quantity;

		@Builder
		public WareUnit(Long wareId, String wareName, Integer quantity) {
			this.wareId = wareId;
			this.wareName = wareName;
			this.quantity = quantity;
		}
	}
}
