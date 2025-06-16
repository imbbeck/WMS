package com.wms.stock.mapper;

import com.wms.stock.domain.model.Stock;
import com.wms.stock.dto.StockResponse;
import org.springframework.stereotype.Component;

@Component
public class StockMapper {
    
    public StockResponse toResponse(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getWare().getId(),
                stock.getWare().getName(),
                stock.getLocation().getId(),
                stock.getLocation().getName(),
                stock.getQuantity(),
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }
} 