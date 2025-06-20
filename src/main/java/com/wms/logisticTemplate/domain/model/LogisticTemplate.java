package com.wms.logisticTemplate.domain.model;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.location.domain.model.Location;
import com.wms.ware.domain.model.Ware;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "logistic_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogisticTemplate extends BaseEntity {
	@Size(max = 255)
	@Column(name = "name", nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private LogisticType type;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ware_id", nullable = false)
	private Ware ware;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "from_location_id", nullable = false)
	private Location fromLocation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "to_location_id", nullable = false)
	private Location toLocation;

	@Column(name = "standard_quantity", nullable = false)
	private Integer standardQuantity;

	@Builder
	public LogisticTemplate(String name, LogisticType type, Ware ware, Location fromLocation, Location toLocation, Integer standardQuantity) {
		validateData(name, type, standardQuantity);
		this.name = name;
		this.type = type;
		this.ware = ware;
		this.fromLocation = fromLocation;
		this.toLocation = toLocation;
		this.standardQuantity = standardQuantity;
	}

	public void update(String name, LogisticType type, Integer standardQuantity) {
		validateData(name, type, standardQuantity);
		this.name = name;
		this.type = type;
		this.standardQuantity = standardQuantity;
	}

	private static void validateData(String name, LogisticType type, Integer standardQuantity) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("물류이동 템플릿 이름은 필수입니다.");
		}

		if (type == null) {
			throw new IllegalArgumentException("물류이동 템플릿 타입은 필수입니다.");
		}

		if (standardQuantity == null || standardQuantity <= 0) {
			throw new IllegalArgumentException("표준 수량은 0보다 커야 합니다.");
		}
	}
}