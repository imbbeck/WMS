package com.wms.movement.domain.event;

import com.wms.movement.domain.model.Movement;
import lombok.Getter;

@Getter
public class MovementCompletedEvent {
    private final Long movementId;
    private final Long wareId;
    private final Long toLocationId;
    private final Integer quantity;

    public MovementCompletedEvent(Movement movement) {
        this.movementId = movement.getId();
        this.wareId = movement.getWare().getId();
        this.toLocationId = movement.getToLocation().getId();
        this.quantity = movement.getQuantity();
    }
} 