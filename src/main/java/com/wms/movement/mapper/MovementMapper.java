package com.wms.movement.mapper;

import com.wms.movement.domain.model.Movement;
import com.wms.movement.dto.MovementResponse;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {
    
    public MovementResponse toResponse(Movement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getWare().getId(),
                movement.getWare().getName(),
                movement.getFromLocation().getId(),
                movement.getFromLocation().getName(),
                movement.getToLocation().getId(),
                movement.getToLocation().getName(),
                movement.getQuantity(),
                movement.getPaletteQuantity(),
                movement.getStatus(),
                movement.getStartedAt(),
                movement.getCompletedAt(),
                movement.getCreatedAt(),
                movement.getUpdatedAt()
        );
    }
} 