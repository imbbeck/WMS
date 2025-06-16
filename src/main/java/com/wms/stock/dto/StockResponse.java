package com.wms.stock.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class StockResponse {
    private final Long id;
    private final Long wareId;
    private final String wareName;
    private final Long locationId;
    private final String locationName;
    private final Integer quantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StockResponse(Long id, Long wareId, String wareName, Long locationId, String locationName,
                        Integer quantity, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.wareId = wareId;
        this.wareName = wareName;
        this.locationId = locationId;
        this.locationName = locationName;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
} 