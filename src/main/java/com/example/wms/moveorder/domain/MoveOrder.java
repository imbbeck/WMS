package com.example.wms.moveorder.domain;

import com.example.wms.location.domain.Location;
import com.example.wms.location.domain.LocationType;
import com.example.wms.ware.domain.Ware;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDate;

@Entity
@Table(name = "move_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MoveOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "move_order_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MoveOrderType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id", nullable = false)
    private Location fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id", nullable = false)
    private Location toLocation;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MoveOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ware_id", nullable = false)
    private Ware ware;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    public MoveOrder(String name, MoveOrderType type, Location fromLocation, Location toLocation, LocalDate scheduledDate, Ware ware, Long quantity) {
        validate(name, type, fromLocation, toLocation, scheduledDate, ware, quantity);

        this.name = name;
        this.type = type;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.scheduledDate = scheduledDate;
        this.status = MoveOrderStatus.PENDING;
        this.ware = ware;
        this.quantity = quantity;
    }

    public void complete() {
        this.status = MoveOrderStatus.COMPLETED;
    }

    public void cancel() {
        this.status = MoveOrderStatus.CANCELLED;
    }

    public void fail() {
        this.status = MoveOrderStatus.FAILED;
    }

    private void validate(String name, MoveOrderType type, Location fromLocation, Location toLocation, LocalDate scheduledDate, Ware ware, Long quantity) {
        Assert.hasText(name, "오더 이름은 필수입니다.");
        Assert.notNull(type, "오더 타입은 필수입니다.");
        Assert.notNull(fromLocation, "출발지는 필수입니다.");
        Assert.notNull(toLocation, "도착지는 필수입니다.");
        Assert.notNull(scheduledDate, "예정일은 필수입니다.");
        Assert.notNull(ware, "상품은 필수입니다.");
        Assert.notNull(quantity, "수량은 필수입니다.");
        Assert.isTrue(quantity > 0, "수량은 0보다 커야합니다.");

        switch (type) {
            case INBOUND:
                Assert.isTrue(fromLocation.getType() == LocationType.INBOUND, "입고 오더의 출발지는 입고처여야 합니다.");
                Assert.isTrue(toLocation.getType() == LocationType.YARD, "입고 오더의 도착지는 야적장이어야 합니다.");
                break;
            case OUTBOUND:
                Assert.isTrue(fromLocation.getType() == LocationType.YARD, "출고 오더의 출발지는 야적장여야 합니다.");
                Assert.isTrue(toLocation.getType() == LocationType.OUTBOUND, "출고 오더의 도착지는 출고처여야 합니다.");
                break;
            case TRANSFER:
                Assert.isTrue(
                        (fromLocation.getType() == LocationType.YARD || fromLocation.getType() == LocationType.WAREHOUSE) &&
                        (toLocation.getType() == LocationType.YARD || toLocation.getType() == LocationType.WAREHOUSE),
                        "내부이동 오더는 야적장/창고 간에만 가능합니다."
                );
                break;
        }
    }
} 