package com.wms.userInfo.domain.exception;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.applicationInfra.exception.BusinessException;
import com.wms.applicationInfra.exception.DomainExceptionHelper;

public class UserInfoException {

    private UserInfoException() {}

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

    public static class WrongPasswordException extends BusinessException.BadRequestException {
        public WrongPasswordException(String message) {
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
    public static NotFoundEx notFound(Long id) {
        return new NotFoundEx(DomainExceptionHelper.notFound(id));
    }

    public static NotFoundEx notFound(String userId) {
        return new NotFoundEx(DomainExceptionHelper.notFound(userId));
    }

    // ConflictException 생성
    public static ConflictEx duplicate(FieldEnum fieldEnum, String value) {
        return new ConflictEx(DomainExceptionHelper.duplicate(fieldEnum, value));
    }

    public static ConflictEx duplicate(FieldEnum fieldEnum) {
        return new ConflictEx(DomainExceptionHelper.duplicate(fieldEnum));
    }

    public static WrongPasswordException wrongPasswordException() {
        return new WrongPasswordException("기존 비밀번호가 일치하지 않습니다.");
    }
} 