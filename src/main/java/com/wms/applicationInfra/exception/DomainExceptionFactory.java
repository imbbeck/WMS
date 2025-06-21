package com.wms.applicationInfra.exception;

import com.wms.applicationInfra.domain.FieldEnum;

public class DomainExceptionFactory {
	protected final String objectName;

	protected DomainExceptionFactory(String objectName) {
		this.objectName = objectName;
	}

	// ValidationException 생성
	public BusinessException.ValidationException validation(String field, String additionalMessage) {
		String message = String.format("%s %s 값이 유효하지 않습니다. %s",
				objectName, field, additionalMessage);
		return new BusinessException.ValidationException(message);
	}

	public BusinessException.ValidationException validation(FieldEnum fieldEnum) {
		String message = String.format("%s %s 값이 유효하지 않습니다.",
				objectName, fieldEnum.getLabel());
		return new BusinessException.ValidationException(message);
	}

	public BusinessException.ValidationException validation(String message) {
		return new BusinessException.ValidationException(message);
	}

	// NotFoundException 생성
	public BusinessException.NotFoundException notFound(Long id) {
		String message = String.format("존재하지 않는 %s입니다: %d", objectName, id);
		return new BusinessException.NotFoundException(message);
	}

	public BusinessException.NotFoundException notFound(String identifier) {
		String message = String.format("존재하지 않는 %s입니다: %s", objectName, identifier);
		return new BusinessException.NotFoundException(message);
	}

	// ConflictException 생성
	public BusinessException.ConflictException duplicate(String field, String value) {
		String message = String.format("이미 존재하는 %s %s 입니다: %s", objectName, field, value);
		return new BusinessException.ConflictException(message);
	}

	// BadRequestException 생성
	public BusinessException.BadRequestException badRequest(String message) {
		return new BusinessException.BadRequestException(String.format("%s: %s", objectName, message));
	}

}
