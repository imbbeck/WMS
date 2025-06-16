package com.wms.stock.domain.model;

import com.wms.common.domain.BaseEntity;
import com.wms.location.domain.model.Location;
import com.wms.ware.domain.model.Ware;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ware_id", nullable = false)
    private Ware ware;  // 물품

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;  // 위치

    @Column(nullable = false)
    private Integer quantity;  // 수량

    @Version
    private Long version;

    public static Stock create(Ware ware, Location location, Integer quantity) {
        Stock stock = new Stock();
        stock.ware = ware;
        stock.location = location;
        stock.quantity = quantity;
        return stock;
    }

    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("추가할 수량은 0 이상이어야 합니다");
        }
        this.quantity += quantity;
    }

    public void subtractQuantity(Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("차감할 수량은 0 이상이어야 합니다");
        }
        if (this.quantity < quantity) {
            throw new IllegalArgumentException("현재 수량보다 많은 수량을 차감할 수 없습니다");
        }
        this.quantity -= quantity;
    }
} 