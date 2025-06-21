package com.wms.stock.domain.model;

import java.time.LocalDateTime;

import com.wms.location.domain.model.Location;
import com.wms.stock.domain.exception.StockExceptions;
import com.wms.ware.domain.model.Ware;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "stocks")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ware_id", nullable = false)
    private Ware ware;  // 물품

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location warehouse;  // 창고

    @Column(nullable = false)
    private Integer quantity;  // 수량

    @Version
    private Long version;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Stock(Ware ware, Location location, Integer quantity) {
        this.ware = ware;
        this.warehouse = location;
        this.quantity = quantity;
    }

    public static Stock create(Ware ware, Location location, Integer quantity) {
        Stock stock = new Stock();
        stock.ware = ware;
        stock.warehouse = location;
        stock.quantity = quantity;
        return stock;
    }

    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(Integer quantity) {
        if (quantity < 0) {
            throw new StockExceptions.InvalidQuantityExceptions(quantity);
        }
        this.quantity += quantity;
    }

    public void removeQuantity(Integer quantity) {
        if (quantity < 0) {
            throw new StockExceptions.InvalidQuantityExceptions(quantity);
        }
        if (this.quantity < quantity) {
            throw new StockExceptions.InsufficientStockExceptions(
                    this.ware.getId(), this.warehouse.getId(), quantity, this.quantity);
        }
        this.quantity -= quantity;
    }
} 