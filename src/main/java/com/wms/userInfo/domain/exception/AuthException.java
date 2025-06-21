package com.wms.userInfo.domain.exception;

import com.wms.applicationInfra.exception.BusinessException;

public final class AuthException {

	private AuthException() {}

	public static class UnauthorizedEx extends BusinessException.UnauthorizedException {
		public UnauthorizedEx(String message) {
			super(message);
		}
	}

	// UnauthorizedException 생성
	public static UnauthorizedEx unauthorize(String message) {
		return new UnauthorizedEx(message);
	}
}
