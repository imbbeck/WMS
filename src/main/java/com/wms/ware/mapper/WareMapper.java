package com.wms.ware.mapper;

import com.wms.ware.domain.model.Ware;
import com.wms.ware.dto.WareRequest;
import com.wms.ware.dto.WareResponse;
import org.springframework.stereotype.Component;

@Component
public class WareMapper {
    
    public Ware toEntity(WareRequest request) {
        return Ware.builder()
                .name(request.name())
                .type(request.type())
                .paletteUnit(request.paletteUnit())
                .build();
    }

    public WareResponse toResponse(Ware ware) {
        return new WareResponse(
                ware.getId(),
                ware.getName(),
                ware.getType(),
                ware.getPaletteUnit(),
                ware.getCreatedAt(),
                ware.getUpdatedAt()
        );
    }
} 