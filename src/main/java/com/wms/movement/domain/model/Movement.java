package com.wms.movement.domain.model;

import com.wms.common.domain.BaseEntity;
import com.wms.location.domain.model.Location;
import com.wms.ware.domain.model.Ware;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ware_id", nullable = false)
    private Ware ware;  // 물품

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id", nullable = false)
    private Location fromLocation;  // 출발 위치

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id", nullable = false)
    private Location toLocation;  // 도착 위치

    @Column(nullable = false)
    private Integer quantity;  // 이동 수량

    @Column(nullable = false)
    private Integer paletteQuantity;  // 이동 파레트 수량

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementStatus status;  // 이동 상태

    @Column
    private LocalDateTime startedAt;  // 이동 시작 시간

    @Column
    private LocalDateTime completedAt;  // 이동 완료 시간

    public static Movement create(Ware ware, Location fromLocation, Location toLocation, Integer quantity) {
        Movement movement = new Movement();
        movement.ware = ware;
        movement.fromLocation = fromLocation;
        movement.toLocation = toLocation;
        movement.quantity = quantity;
        movement.status = MovementStatus.PENDING;
        return movement;
    }

    public void start() {
        if (this.status != MovementStatus.PENDING) {
            throw new IllegalStateException("대기 중인 이동 작업만 시작할 수 있습니다");
        }
        this.status = MovementStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != MovementStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 이동 작업만 완료할 수 있습니다");
        }
        this.status = MovementStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == MovementStatus.COMPLETED) {
            throw new IllegalStateException("완료된 이동 작업은 취소할 수 없습니다");
        }
        this.status = MovementStatus.CANCELLED;
    }
} 