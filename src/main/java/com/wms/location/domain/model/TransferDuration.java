package com.wms.location.domain.model;

import com.wms.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transfer_duration")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferDuration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id", nullable = false)
    private Location fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id", nullable = false)
    private Location toLocation;

    @Column(nullable = false)
    private Integer estimatedDuration;

    @Builder
    public TransferDuration(Location fromLocation, Location toLocation, Integer estimatedDuration) {
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.estimatedDuration = estimatedDuration;
    }

    public void validateLocations() {
        if (fromLocation.equals(toLocation)) {
            throw new IllegalArgumentException("출발지와 도착지가 같을 수 없습니다.");
        }
    }

    public void update(Location fromLocation, Location toLocation, Integer durationMinutes) {
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.estimatedDuration = durationMinutes;
    }
} 