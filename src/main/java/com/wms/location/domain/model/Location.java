package com.wms.location.domain.model;

import com.wms.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    @Column
    private Integer capacity;  // WAREHOUSE 타입일 때만 유효

    @Builder
    public Location(String name, LocationType type, Integer capacity) {
        validateLocationData(name, type, capacity);
        this.name = name;
        this.type = type;
        this.capacity = capacity;
    }

    // 검증 로직을 별도 메서드로 분리
    private static void validateLocationData(String name, LocationType type, Integer capacity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("위치 이름은 필수입니다.");
        }

        if (type == null) {
            throw new IllegalArgumentException("위치 타입은 필수입니다.");
        }

        if (type == LocationType.WAREHOUSE) {
            if (capacity == null || capacity <= 0) {
                throw new IllegalArgumentException("창고의 용량은 0보다 커야 합니다.");
            }
        } else {
            if (capacity != null) {
                throw new IllegalArgumentException("입고/출고처는 용량을 지정할 수 없습니다.");
            }
        }
    }

    public boolean isWarehouse() {
        return this.type == LocationType.WAREHOUSE;
    }

    public boolean isInbound() {
        return this.type == LocationType.INBOUND;
    }

    public boolean isOutbound() {
        return this.type == LocationType.OUTBOUND;
    }

    public void validateCapacity(int quantity) {
        if (isWarehouse() && capacity != null && quantity > capacity) {
            throw new IllegalArgumentException("창고 용량을 초과할 수 없습니다.");
        }
    }

    public void update(String name, LocationType type) {
        this.name = name;
        this.type = type;
    }
} 