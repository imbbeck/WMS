package com.example.wms.warehouse;

import com.example.wms.enums.WarehouseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "warehouse")
public class Warehouse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long warehouseId;

	private String name;

	private String location;

	@Enumerated(EnumType.STRING)
	private WarehouseStatus status;

	private int capacity;

	private int usedCapacity;

	@Builder
	public Warehouse(String name, String location, int capacity) {
		this.name = name;
		this.location = location;
		this.capacity = capacity;
		this.usedCapacity = 0;
		this.status = WarehouseStatus.AVAILABLE;
	}

	public void addInventory(int quantity) {
		if (usedCapacity + quantity > capacity) {
			throw new IllegalStateException("용량 초과");
		}
		usedCapacity += quantity;
		updateStatusByCapacity();
	}

	public void removeInventory(int quantity) {
		if (usedCapacity - quantity < 0) {
			throw new IllegalStateException("재고 부족");
		}
		usedCapacity -= quantity;
		updateStatusByCapacity();
	}

	private void updateStatusByCapacity() {
		if (usedCapacity == capacity) {
			status = WarehouseStatus.FULL;
		} else {
			status = WarehouseStatus.AVAILABLE;
		}
	}

	public void changeStatus(WarehouseStatus newStatus) {
		if (newStatus == WarehouseStatus.FULL) {
			//FULL 상태는 외부에서 직접 설정할 수 없으며, 내부 로직에 의해서만 설정.
			throw new IllegalArgumentException("FULL 상태는 직접 변경 불가");
		}
		this.status = newStatus;
	}

	public void updateInfo(String name, String location) {this.name = name; this.location = location;}


}
