package com.example.wms.moveorder.dto;

import com.example.wms.moveorder.domain.MoveOrder;
import com.example.wms.moveorder.domain.MoveOrderStatus;
import com.example.wms.moveorder.domain.MoveOrderType;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MoveOrderResponse {
    private final Long id;
    private final String name;
    private final MoveOrderType type;
    private final LocationInfo fromLocation;
    private final LocationInfo toLocation;
    private final LocalDate scheduledDate;
    private final MoveOrderStatus status;
    private final WareInfo ware;
    private final Long quantity;

    public MoveOrderResponse(MoveOrder moveOrder) {
        this.id = moveOrder.getId();
        this.name = moveOrder.getName();
        this.type = moveOrder.getType();
        this.fromLocation = new LocationInfo(moveOrder.getFromLocation().getId(), moveOrder.getFromLocation().getName());
        this.toLocation = new LocationInfo(moveOrder.getToLocation().getId(), moveOrder.getToLocation().getName());
        this.scheduledDate = moveOrder.getScheduledDate();
        this.status = moveOrder.getStatus();
        this.ware = new WareInfo(moveOrder.getWare().getId(), moveOrder.getWare().getName());
        this.quantity = moveOrder.getQuantity();
    }

    @Getter
    private static class LocationInfo {
        private final Long id;
        private final String name;

        public LocationInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Getter
    private static class WareInfo {
        private final Long id;
        private final String name;

        public WareInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
} 