package com.wms.movement.domain.event;

import com.wms.movement.domain.model.Movement;
import lombok.Getter;

@Getter
public class MovementStartedEvent {
    private final Long movementId;
    private final Long wareId;
    private final Long fromLocationId;
    private final Integer quantity;

    public MovementStartedEvent(Movement movement) {
        this.movementId = movement.getId();
        this.wareId = movement.getWare().getId();
        this.fromLocationId = movement.getFromLocation().getId();
        this.quantity = movement.getQuantity();
    }
} 