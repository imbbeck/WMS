package com.wms.location.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransferDurationRequest {
    
    @NotNull(message = "출발지 ID는 필수입니다")
    private Long fromLocationId;
    
    @NotNull(message = "도착지 ID는 필수입니다")
    private Long toLocationId;
    
    @NotNull(message = "예상소요시간은 필수입니다")
    @Positive(message = "예상소요시간은 0보다 커야 합니다")
    private Integer estimatedDuration;
} 