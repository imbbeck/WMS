package com.wms.stock.domain.exception;

public class StockException extends RuntimeException {
    public StockException(String message) {
        super(message);
    }

    public static class InsufficientStockException extends StockException {
        public InsufficientStockException(Long wareId, Long locationId, int requested, int available) {
            super(String.format("재고 부족: 물품(%d) 위치(%d) 요청수량(%d) 가용수량(%d)", 
                wareId, locationId, requested, available));
        }
    }

    public static class WarehouseCapacityExceededException extends StockException {
        public WarehouseCapacityExceededException(Long locationId, int requested, int capacity) {
            super(String.format("창고 용량 초과: 위치(%d) 요청수량(%d) 최대용량(%d)", 
                locationId, requested, capacity));
        }
    }
} 