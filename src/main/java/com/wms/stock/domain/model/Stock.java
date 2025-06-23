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

	/**
	 * 재고 수량 업데이트 - 0이면 삭제 마킹
	 * @return true: 삭제 필요, false: 업데이트만
	 */
	public boolean updateQuantityAndCheckDeletion(Integer newQuantity) {
		if (newQuantity < 0) {
			throw StockException.cannotNegativeQuantityEx(newQuantity);
		}

		this.quantity = newQuantity;

		// 0이면 삭제 필요함을 반환
		return newQuantity == 0;
	}

	/**
	 * 재고 증가
	 * 재고 증가 시에는 재고 수량 이 음수로 내려갈 수 없으므로 void. addedQuantity는 DTO에서 @Positive 로 검증됨.
	 */
	public void plusQuantityWithCapacityCheck(int warehouseCapacity, int currentSum , int addedQuantity) {
		if (currentSum  + addedQuantity > warehouseCapacity) {
			throw LocationException.warehouseCapacityExceeded(this.warehouseId, warehouseCapacity, currentSum , addedQuantity);
		}
		this.quantity += addedQuantity;
	}

	/**
	 * 재고 감소
	 * @return true: 삭제 필요, false: 업데이트만
	 */
	public boolean minusQuantityWithStockCheck(int oldQuantity, int removeQuantity) {
		if (oldQuantity < removeQuantity) {
			throw StockException.insufficientStock(this.warehouseId, this.wareId, oldQuantity, removeQuantity
			);
		}
		this.quantity -= removeQuantity;

		return updateQuantityAndCheckDeletion(this.quantity);

	}
}