package com.wms.ware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WareUpdateRequest(
    @NotBlank(message = "물품명은 필수입니다")
    String name,

    @NotBlank(message = "물품 타입은 필수입니다")
    String type,

    @NotNull(message = "파레트 당 물품 개수는 필수입니다")
    @Positive(message = "파레트 당 물품 개수는 0보다 커야 합니다")
    Integer paletteUnit
) {} 