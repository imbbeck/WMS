package com.example.wms.inventory;

import com.example.wms.ware.Ware;
import com.example.wms.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "inventory",
		uniqueConstraints = {@UniqueConstraint(columnNames = {"warehouse_id", "ware_id"})})
public class Inventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long inventoryId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id")
	private Warehouse warehouse;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ware_id")
	private Ware ware;

	private int quantity;

	protected Inventory() {}

	public Inventory(Warehouse warehouse, Ware ware, int quantity) {
		this.warehouse = warehouse;
		this.ware = ware;
		this.quantity = quantity;
	}

	public void increaseQuantity(int amount) {
		this.quantity += amount;
	}

	public void decreaseQuantity(int amount) {
		if (this.quantity - amount < 0) {
			throw new IllegalStateException("재고 부족");
		}
		this.quantity -= amount;
	}

}
