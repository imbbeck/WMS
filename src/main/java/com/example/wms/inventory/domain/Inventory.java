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
@EntityListeners(InventoryChangeListener.class)
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

    public static Inventory create(Location location, Ware ware) {
        return new Inventory(location, ware, 0L);
    }

    private Inventory(Location location, Ware ware, Long quantity) {
        Assert.notNull(location, "위치는 필수입니다.");
        Assert.notNull(ware, "상품은 필수입니다.");
        Assert.notNull(quantity, "수량은 필수입니다.");
        Assert.isTrue(quantity >= 0, "수량은 0 이상이어야 합니다.");

        this.location = location;
        this.ware = ware;
        this.quantity = quantity;
    }

    public void increase(Long quantity) {
        Assert.notNull(quantity, "수량은 필수입니다.");
        Assert.isTrue(quantity > 0, "수량은 0보다 커야 합니다.");
        this.quantity += quantity;
    }

    public void decrease(Long quantity) {
        Assert.notNull(quantity, "수량은 필수입니다.");
        Assert.isTrue(quantity > 0, "수량은 0보다 커야 합니다.");
        Assert.isTrue(this.quantity >= quantity, "재고가 부족합니다.");
        this.quantity -= quantity;
    }
} 