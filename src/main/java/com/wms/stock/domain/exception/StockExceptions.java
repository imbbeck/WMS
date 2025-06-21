package com.wms.stock.domain.exception;

import com.wms.applicationInfra.exception.BusinessExceptions;

public class StockExceptions extends BusinessExceptions {
    public StockExceptions(String message) {
        super(message);
    }

    public static class InsufficientStockExceptions extends StockExceptions {
        public InsufficientStockExceptions(Long wareId, Long locationId, int requested, int available) {
            super(String.format("재고 부족: 물품(%d) 장소(%d) 요청수량(%d) 가용수량(%d)", 
                wareId, locationId, requested, available));
        }
    }

    public static class WarehouseCapacityExceededExceptions extends StockExceptions {
        public WarehouseCapacityExceededExceptions(Long locationId, int requested, int capacity) {
            super(String.format("창고 용량 초과: 장소(%d) 요청수량(%d) 최대용량(%d)", 
                locationId, requested, capacity));
        }
    }

    public static class NotFoundExceptions extends StockExceptions {
        public NotFoundExceptions(Long stockId) {
            super(String.format("존재하지 않는 재고입니다: %d", stockId));
        }
    }

    public static class InvalidQuantityExceptions extends StockExceptions {
        public InvalidQuantityExceptions(int quantity) {
            super(String.format("유효하지 않은 수량입니다: %d (0 이상이어야 함)", quantity));
        }
    }

    public static class InvalidWareIdExceptions extends StockExceptions {
        public InvalidWareIdExceptions() {
            super("물품 ID는 필수입니다.");
        }
    }

    public static class InvalidLocationIdExceptions extends StockExceptions {
        public InvalidLocationIdExceptions() {
            super("장소 ID는 필수입니다.");
        }
    }
} 