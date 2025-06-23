package com.wms.location.domain.exception;

import java.util.List;
import java.util.stream.Collectors;

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

    // CannotDeleteWithStockEx
    public static class CannotDeleteWithStockEx extends BusinessException.ConflictException {
        public CannotDeleteWithStockEx(String message) {
            super(message);
        }
    }

    public static CannotDeleteWithStockEx cannotDeleteWithStock(long warehouseId, List<Long> relatedIdList) {
        String relatedIds = relatedIdList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return new CannotDeleteWithStockEx(String.format("창고 삭제 불가: 대상창고에 재고가 존재합니다. 위치(%d) 재고 목록(%s)",
                warehouseId, relatedIds));
    }

    // cannotDeleteLocationWithTaskEx
    public static class CannotDeleteLocationWithTaskEx extends BusinessException.ConflictException {
        public CannotDeleteLocationWithTaskEx(String message) {
            super(message);
        }
    }

    public static CannotDeleteLocationWithTaskEx cannotDeleteLocationWithTasks(long warehouseId, List<Long> relatedIdList) {
        String relatedIds = relatedIdList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return new CannotDeleteLocationWithTaskEx(String.format("창고 삭제 불가: 대상창고을 시작/도착으로 계획된 물류작업이 존재합니다. 위치(%d) 계획된 물류작업 목록(%s)",
                warehouseId, relatedIds));
    }

    // CannotDeleteLocationWithTemplateEx
    public static class CannotDeleteLocationWithTemplateEx extends BusinessException.ConflictException {
        public CannotDeleteLocationWithTemplateEx(String message) {
            super(message);
        }
    }

    public static CannotDeleteLocationWithTemplateEx cannotDeleteLocationWithTemplates(long warehouseId, List<Long> relatedIdList) {
        String relatedIds = relatedIdList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return new CannotDeleteLocationWithTemplateEx(String.format("창고 삭제 불가: 대상창고을 시작/도착으로 하는 물류템플릿이 존재합니다. 위치(%d) 물류템플릿 목록(%s)",
                warehouseId, relatedIds));
    }

}