package com.wms.stock.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.location.domain.exception.LocationException;
import com.wms.stock.domain.exception.StockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks",
		uniqueConstraints = @UniqueConstraint(columnNames = {"ware_id", "location_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {
	// 매핑 걷어냄. Stock은 단순한 n:n 매핑 테이블이 아니라 상태(수량)를 가지며, 자체 비즈니스 로직도 가질 수 있는 주체적 Aggregate
	// 재고는 자신의 상태 중심으로 동작하며, 연관 정보는 클라이언트 조회 시에만 필요하므로, 매핑은 오히려 손해

	@Column(name = "ware_id", nullable = false)
	private Long wareId;  // 물품

	@Column(name = "location_id", nullable = false)
	private Long warehouseId;  // 창고

	@Column(name = "quantity", nullable = false)
	private Integer quantity;  // 수량

	@Version
	private Long version;

	@Builder
	public Stock(Long wareId, Long warehouseId, Integer quantity) {
		this.wareId = wareId;
		this.warehouseId = warehouseId;
		this.quantity = quantity;
	}

	public void updateQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public void addQuantityWithCapacityCheck(int warehouseCapacity, int currentPalletCount, int addedQuantity) {
		if (currentPalletCount + addedQuantity > warehouseCapacity) {
			throw LocationException.warehouseCapacityExceeded(this.warehouseId, warehouseCapacity, currentPalletCount, addedQuantity);
		}
		this.quantity += addedQuantity;
	}

	public void removeQuantityWithStockCheck(int currentStock, int removeQuantity) {
		if (currentStock < removeQuantity) {
			throw StockException.insufficientStock(this.warehouseId, this.wareId, currentStock, removeQuantity
			);
		}
		this.quantity -= removeQuantity;
	}
}