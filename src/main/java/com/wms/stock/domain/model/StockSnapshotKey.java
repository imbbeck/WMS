package com.wms.stock.domain.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class StockSnapshotKey {
	@Column(name = "ware_id", nullable = false)
	private Long wareId;

	@Column(name = "location_id", nullable = false)
	private Long warehouseId;

	@Column(name = "snapshot_date", nullable = false)
	private LocalDate snapshotDate;
}
