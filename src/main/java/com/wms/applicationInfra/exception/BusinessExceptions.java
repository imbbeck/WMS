package com.wms.applicationInfra.exception;

import org.springframework.http.HttpStatus;

public class BusinessExceptions extends RuntimeException {
    private final HttpStatus httpStatus;

    public BusinessExceptions(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessExceptions(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessExceptions(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public BusinessExceptions(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    // 공통 예외 클래스들을 public static으로 변경
    public static class BadRequestExceptions extends BusinessExceptions {
        public BadRequestExceptions(String message) {
            super(message, HttpStatus.BAD_REQUEST);
        }
    }

    public static class NotFoundExceptions extends BusinessExceptions {
        public NotFoundExceptions(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    public static class ConflictExceptions extends BusinessExceptions {
        public ConflictExceptions(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    public static class UnauthorizedExceptions extends BusinessExceptions {
        public UnauthorizedExceptions(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }

    public static class ForbiddenExceptions extends BusinessExceptions {
        public ForbiddenExceptions(String message) {
            super(message, HttpStatus.FORBIDDEN);
        }
    }
}