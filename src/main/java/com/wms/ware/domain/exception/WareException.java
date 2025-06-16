package com.wms.ware.domain.exception;

import com.wms.common.exception.BusinessException;

public class WareException extends BusinessException {
    public WareException(String message) {
        super(message);
    }

    public static class NotFoundException extends WareException {
        public NotFoundException(Long wareId) {
            super(String.format("존재하지 않는 물품입니다: %d", wareId));
        }
    }

    public static class InvalidQuantityException extends WareException {
        public InvalidQuantityException(Long wareId, int quantity) {
            super(String.format("잘못된 물품 수량입니다: 물품(%d) 수량(%d)", wareId, quantity));
        }
    }
} 