package com.wms.movement.domain.exception;

import com.wms.Infra.exception.BusinessException;

public class MovementException extends BusinessException {
    public MovementException(String message) {
        super(message);
    }

    public static class InvalidStatusException extends MovementException {
        public InvalidStatusException(String currentStatus, String requiredStatus) {
            super(String.format("잘못된 상태 변경: 현재 상태(%s), 요구 상태(%s)", currentStatus, requiredStatus));
        }
    }

    public static class InvalidPathException extends MovementException {
        public InvalidPathException(String movementType, String fromLocationType, String toLocationType) {
            super(String.format("잘못된 이동 경로: 이동 유형(%s), 출발지 유형(%s), 도착지 유형(%s)", 
                movementType, fromLocationType, toLocationType));
        }
    }

    public static class NotFoundException extends MovementException {
        public NotFoundException(Long movementId) {
            super(String.format("존재하지 않는 물류이동입니다: %d", movementId));
        }
    }
} 