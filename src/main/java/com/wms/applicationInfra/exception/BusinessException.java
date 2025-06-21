package com.wms.applicationInfra.exception;

import org.springframework.http.HttpStatus;

public interface BusinessException {
	String getMessage();
	HttpStatus getHttpStatus();

	// 기본 구현체들
	abstract class AbstractBusinessException extends RuntimeException implements BusinessException {
		private final HttpStatus httpStatus;

		protected AbstractBusinessException(String message, HttpStatus httpStatus) {
			super(message);
			this.httpStatus = httpStatus;
		}

		protected AbstractBusinessException(String message, Throwable cause, HttpStatus httpStatus) {
			super(message, cause);
			this.httpStatus = httpStatus;
		}

		@Override
		public HttpStatus getHttpStatus() {
			return httpStatus;
		}
	}

	// 공통 예외 클래스들
	class ValidationException extends AbstractBusinessException {
		public ValidationException(String message) {
			super(message, HttpStatus.BAD_REQUEST);
		}
	}

	class BadRequestException extends AbstractBusinessException {
		public BadRequestException(String message) {
			super(message, HttpStatus.BAD_REQUEST);
		}
	}

	class NotFoundException extends AbstractBusinessException {
		public NotFoundException(String message) {
			super(message, HttpStatus.NOT_FOUND);
		}
	}

	class ConflictException extends AbstractBusinessException {
		public ConflictException(String message) {
			super(message, HttpStatus.CONFLICT);
		}
	}

	class UnauthorizedException extends AbstractBusinessException {
		public UnauthorizedException(String message) {
			super(message, HttpStatus.UNAUTHORIZED);
		}
	}

	class ForbiddenException extends AbstractBusinessException {
		public ForbiddenException(String message) {
			super(message, HttpStatus.FORBIDDEN);
		}
	}
}