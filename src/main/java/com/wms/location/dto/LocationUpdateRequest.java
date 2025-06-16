package com.wms.location.dto;

import com.wms.location.domain.model.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LocationUpdateRequest {
    
    @NotBlank(message = "장소 이름은 필수입니다")
    private String name;
    
    @NotNull(message = "장소 타입은 필수입니다")
    private LocationType type;
    
    private Integer capacity;  // WAREHOUSE 타입일 때만 유효
} 