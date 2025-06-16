package com.wms.location.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class TransferDurationUpdateRequest {
    
    @NotNull(message = "예상소요시간은 필수입니다")
    @Positive(message = "예상소요시간은 0보다 커야 합니다")
    private Integer estimatedDuration;
} 