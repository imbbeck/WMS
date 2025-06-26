package com.wms.stock.domain.model;

import com.wms.logisticTask.application.LogisticTaskValidationService;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class StockKey implements Serializable {

    @Column(name = "ware_id", nullable = false)
    private Long wareId;

    @Column(name = "location_id", nullable = false) 
    private Long warehouseId;

    /**
     * 정적 팩토리 메서드
     */
    public static StockKey of(Long wareId, Long warehouseId) {
        return new StockKey(wareId, warehouseId);
    }

    /**
     * 캐시 키 생성 (일관된 형식)
     */
    public String toCacheKey() {
        return String.format("current_stock:%d:%d", warehouseId, wareId);
    }

    @Override
    public String toString() {
        return String.format("(wareId=%d, warehouseId=%d)", wareId, warehouseId);
    }
}