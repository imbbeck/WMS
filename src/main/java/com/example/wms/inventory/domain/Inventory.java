package com.example.wms.inventory.domain;

import com.example.wms.location.domain.Location;
import com.example.wms.ware.domain.Ware;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Table(name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "inventory_uk",
                        columnNames = {"location_id", "ware_id"}
                )
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ware_id", nullable = false)
    private Ware ware;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    public Inventory(Location location, Ware ware, Long quantity) {
        validate(location, ware, quantity);
        this.location = location;
        this.ware = ware;
        this.quantity = quantity;
    }

    public void increaseQuantity(Long quantity) {
        Assert.notNull(quantity, "수량은 필수입니다.");
        Assert.isTrue(quantity > 0, "증가시킬 수량은 0보다 커야 합니다.");
        this.quantity += quantity;
    }

    public void decreaseQuantity(Long quantity) {
        Assert.notNull(quantity, "수량은 필수입니다.");
        Assert.isTrue(quantity > 0, "감소시킬 수량은 0보다 커야 합니다.");
        if (this.quantity - quantity < 0) {
            throw new IllegalArgumentException("재고는 음수가 될 수 없습니다.");
        }
        this.quantity -= quantity;
    }

    private void validate(Location location, Ware ware, Long quantity) {
        Assert.notNull(location, "장소는 필수입니다.");
        Assert.notNull(ware, "물품은 필수입니다.");
        Assert.notNull(quantity, "수량은 필수입니다.");
        Assert.isTrue(quantity >= 0, "재고 수량은 0보다 작을 수 없습니다.");
    }
} 