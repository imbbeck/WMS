package com.wms.ware.dto;

import java.time.LocalDateTime;

public record WareResponse(
    Long id,
    String name,
    String type,
    Integer paletteUnit,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {} 