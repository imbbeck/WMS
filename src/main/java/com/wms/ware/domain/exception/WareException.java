package com.wms.ware.domain.exception;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.applicationInfra.exception.BusinessException;
import com.wms.applicationInfra.exception.DomainExceptionHelper;
import com.wms.location.domain.exception.LocationException;

public class WareException {

    private WareException() {}

    // 도메인별 예외 클래스들
    public static class ValidationEx extends BusinessException.ValidationException {
        public ValidationEx(String message) {
            super(message);
        }
    }

    public static class NotFoundEx extends BusinessException.NotFoundException {
        public NotFoundEx(String message) {
            super(message);
        }
    }

    public static class ConflictEx extends BusinessException.ConflictException {
        public ConflictEx(String message) {
            super(message);
        }
    }

    // ValidationException 생성
    public static ValidationEx validation(String field, String additionalMessage) {
        return new ValidationEx(DomainExceptionHelper.validation(field, additionalMessage));
    }

    public static ValidationEx validation(FieldEnum fieldEnum) {
        return new ValidationEx(DomainExceptionHelper.validation(fieldEnum));
    }

    public static ValidationEx validation(String message) {
        return new ValidationEx(message);
    }

    // NotFoundException 생성
    public static NotFoundEx notFound(Long locationId) {
        return new NotFoundEx(DomainExceptionHelper.notFound(locationId));
    }

    // ConflictException 생성
    public static ConflictEx duplicate(FieldEnum fieldEnum, String value) {
        return new ConflictEx(DomainExceptionHelper.duplicate(fieldEnum, value));
    }

} 