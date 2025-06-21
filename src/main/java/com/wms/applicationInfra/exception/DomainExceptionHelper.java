package com.wms.applicationInfra.exception;

import com.wms.applicationInfra.domain.FieldEnum;

public final class DomainExceptionHelper {

	private DomainExceptionHelper() {}

	// 공통 메시지 포맷팅 유틸리티 메서드들
	public static String validation(String field, String additionalMessage) {
		return String.format("%s 값이 유효하지 않습니다. %s", field, additionalMessage);
	}

	public static String validation(FieldEnum fieldEnum) {
		return String.format("%s 값이 유효하지 않습니다.", fieldEnum.getLabel());
	}

	public static String notFound(Long id) {
		return String.format("존재하지 않는 데이터입니다: %d", id);
	}

	public static String notFound(String identifier) {
		return String.format("존재하지 않는 데이터입니다: %s", identifier);
	}

	public static String duplicate(FieldEnum fieldEnum, String value) {
		return String.format("이미 존재하는 %s 입니다: %s", fieldEnum.getLabel(), value);
	}

	public static String duplicate(FieldEnum fieldEnum) {
		return String.format("이미 존재하는 %s 입니다.", fieldEnum.getLabel());
	}

}
