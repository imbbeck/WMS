package com.example.wms.locationtransfertime.domain;

import com.example.wms.location.domain.Location;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Table(name = "location_transfer_time")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LocationTransferTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_transfer_time_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id", nullable = false)
    private Location fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id", nullable = false)
    private Location toLocation;

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    public LocationTransferTime(Location fromLocation, Location toLocation, Integer durationMin) {
        validate(fromLocation, toLocation, durationMin);
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.durationMin = durationMin;
    }

    public void updateDuration(Integer durationMin) {
        Assert.notNull(durationMin, "이동 시간은 필수입니다.");
        Assert.isTrue(durationMin > 0, "이동 시간은 0보다 커야합니다.");
        this.durationMin = durationMin;
    }

    private void validate(Location fromLocation, Location toLocation, Integer durationMin) {
        Assert.notNull(fromLocation, "출발지는 필수입니다.");
        Assert.notNull(toLocation, "도착지는 필수입니다.");
        Assert.notNull(durationMin, "이동 시간은 필수입니다.");
        Assert.isTrue(durationMin > 0, "이동 시간은 0보다 커야합니다.");
    }
} 