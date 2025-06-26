package com.wms.stock.domain.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
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
	@Embedded
	private StockKey stockKey;

	@Column(name = "snapshot_date", nullable = false)
	private LocalDate snapshotDate;

	/**
	 * 정적 팩토리 메서드
	 */
	public static StockSnapshotKey of(StockKey stockKey, LocalDate snapshotDate) {
		return new StockSnapshotKey(stockKey, snapshotDate);
	}

}
