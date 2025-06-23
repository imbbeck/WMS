package com.wms.stock.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "stock_daily_snapshot",
		uniqueConstraints = @UniqueConstraint(columnNames = {"ware_id", "location_id", "snapshot_date"}))
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockDailySnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Embedded
	private StockSnapshotKey key;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "change_from_yesterday")
	private Integer changeFromYesterday;

	@Builder
	public StockDailySnapshot(StockSnapshotKey key, Integer quantity, Integer changeFromYesterday) {
		this.key = key;
		this.quantity = quantity;
		this.changeFromYesterday = changeFromYesterday;
	}

}

