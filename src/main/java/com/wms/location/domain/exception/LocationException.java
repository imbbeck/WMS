package com.wms.location.domain.exception;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.applicationInfra.exception.BusinessException;
import com.wms.applicationInfra.exception.DomainExceptionHelper;

public final class LocationException {

    private LocationException() {}

    // ValidationEx
    public static class ValidationEx extends BusinessException.ValidationException {
        public ValidationEx(String message) {
            super(message);
        }
    }

    public static ValidationEx validation(String field, String additionalMessage) {
        return new ValidationEx(DomainExceptionHelper.validation(field, additionalMessage));
    }

    public static ValidationEx validation(FieldEnum fieldEnum) {
        return new ValidationEx(DomainExceptionHelper.validation(fieldEnum));
    }

    public static ValidationEx validation(String message) {
        return new ValidationEx(message);
    }

    // NotFoundEx
    public static class NotFoundEx extends BusinessException.NotFoundException {
        public NotFoundEx(String message) {
            super(message);
        }
    }

    public static NotFoundEx notFound(Long locationId) {
        return new NotFoundEx(DomainExceptionHelper.notFound(locationId));
    }


    // ConflictEx
    public static class ConflictEx extends BusinessException.ConflictException {
        public ConflictEx(String message) {
            super(message);
        }
    }

    public static ConflictEx duplicate(FieldEnum fieldEnum, String value) {
        return new ConflictEx(DomainExceptionHelper.duplicate(fieldEnum, value));
    }

    // NotWarehouseEx
    public static class NotWarehouseEx extends BusinessException.ConflictException {
        public NotWarehouseEx(String message) {
            super(message);
        }
    }

    public static NotWarehouseEx notWarehouseEx(Long locationId) {
        return new NotWarehouseEx(String.format("위치(%d)는 창고가 아닙니다.", locationId));
    }

    // WarehouseCapacityExceededEx
    public static class WarehouseCapacityExceededEx extends BusinessException.ConflictException {
        public WarehouseCapacityExceededEx(String message) {
            super(message);
        }
    }

    public static WarehouseCapacityExceededEx warehouseCapacityExceeded(long warehouseId, int capacity, int currentPalletCount, int addedQuantity) {
        return new WarehouseCapacityExceededEx(String.format("창고 용량 초과: 위치(%d) 최대 수용량(%d) 요청 수량(%d) 현재 용량(%d)",
                warehouseId, capacity, currentPalletCount, addedQuantity));
    }
}