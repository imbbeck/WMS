package com.wms.location.domain.exception;

import com.wms.common.exception.BusinessException;

public class LocationException extends BusinessException {
    public LocationException(String message) {
        super(message);
    }

    public static class NotFoundException extends LocationException {
        public NotFoundException(Long locationId) {
            super(String.format("존재하지 않는 위치입니다: %d", locationId));
        }
    }

    public static class InvalidTypeException extends LocationException {
        public InvalidTypeException(String locationType) {
            super(String.format("잘못된 위치 유형입니다: %s", locationType));
        }
    }

    public static class CapacityExceededException extends LocationException {
        public CapacityExceededException(Long locationId, int requested, int capacity) {
            super(String.format("위치 용량 초과: 위치(%d) 요청수량(%d) 최대용량(%d)", 
                locationId, requested, capacity));
        }
    }
} 