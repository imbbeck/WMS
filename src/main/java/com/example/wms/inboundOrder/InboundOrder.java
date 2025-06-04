package com.example.wms.inboundOrder;

import java.time.LocalDateTime;

import com.example.wms.enums.OrderStatus;
import com.example.wms.ware.Ware;
import com.example.wms.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbound_order")
public class InboundOrder { // 창고 간 이동, 배송물품 창고로 이동 등 내부 주문
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_warehouse_id")
	private Warehouse sourceWarehouse;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_warehouse_id")
	private Warehouse targetWarehouse;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ware_id")
	private Ware ware;

	private int quantity;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	// 생성자에 @Builder 적용하면 의도하지 않는 필드도 set될 수 있으므로
	public InboundOrder(Warehouse sourceWarehouse, Warehouse targetWarehouse, Ware ware, int quantity) {
		this.sourceWarehouse = sourceWarehouse;
		this.targetWarehouse = targetWarehouse;
		this.ware = ware;
		this.quantity = quantity;
		this.status = OrderStatus.IN_PROGRESS;
		this.createdAt = LocalDateTime.now();
	}

	// 정적 팩토리 메서드에 @Builder 적용
	@Builder(builderMethodName = "createOrder")
	public static InboundOrder createOrder(Warehouse sourceWarehouse, Warehouse targetWarehouse, Ware ware, int quantity) {
		return new InboundOrder(sourceWarehouse, targetWarehouse, ware, quantity);
	}

	public void updateStatus(OrderStatus newStatus) {
		this.status = newStatus;
		this.updatedAt = LocalDateTime.now();
	}

}
