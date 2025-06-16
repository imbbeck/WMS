package com.wms.movement.dto;

import com.wms.movement.domain.model.MovementStatus;

import java.time.LocalDateTime;

public record MovementResponse(
    Long id,
    Long wareId,
    String wareName,
    Long fromLocationId,
    String fromLocationName,
    Long toLocationId,
    String toLocationName,
    Integer quantity,
    Integer paletteQuantity,
    MovementStatus status,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {} 