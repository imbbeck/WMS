package com.example.wms.outboundOrder;

import java.time.LocalDateTime;

import com.example.wms.enums.OrderStatus;
import com.example.wms.enums.OutboundOrderType;
import com.example.wms.ware.Ware;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbound_order") // order는 예약어라 orders로
public class OutboundOrder { // supplier

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderId;

	@Enumerated(EnumType.STRING)
	private OutboundOrderType orderType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ware_id")
	private Ware ware;

	private int orderQuantity;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	private LocalDateTime orderDate;

	private LocalDateTime deliveryDate;

	// 생성자에 @Builder 적용하면 의도하지 않는 필드도 set될 수 있으므로
	public OutboundOrder(OutboundOrderType orderType,  Ware ware, int orderQuantity) {
		this.orderType = orderType;
		this.ware = ware;
		this.orderQuantity = orderQuantity;
		this.status = OrderStatus.IN_PROGRESS;
		this.orderDate = LocalDateTime.now();
	}

	// 정적 팩토리 메서드에 @Builder 적용
	@Builder(builderMethodName = "createOrder")
	public static OutboundOrder createOrder(OutboundOrderType orderType,Ware ware, int orderQuantity) {
		return new OutboundOrder(orderType, ware, orderQuantity);
	}

	public void updateStatus(OrderStatus newStatus) {
		this.status = newStatus;
		if (newStatus == OrderStatus.COMPLETED) {
			this.deliveryDate = LocalDateTime.now();
		}
	}

}
