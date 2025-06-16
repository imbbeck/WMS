package com.wms.movement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovementRequest(
    @NotNull(message = "물품 ID는 필수입니다")
    Long wareId,

    @NotNull(message = "출발 위치 ID는 필수입니다")
    Long fromLocationId,

    @NotNull(message = "도착 위치 ID는 필수입니다")
    Long toLocationId,

    @NotNull(message = "이동 수량은 필수입니다")
    @Positive(message = "이동 수량은 0보다 커야 합니다")
    Integer quantity
) {} 