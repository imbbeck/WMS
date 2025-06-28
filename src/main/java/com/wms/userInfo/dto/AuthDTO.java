package com.wms.userInfo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDTO {

	@Getter
	@NoArgsConstructor
	@Schema(description = "로그인 요청")
	public static class LoginReq {

		@NotBlank(message = "사용자 ID는 필수입니다")
		@Schema(description = "사용자 ID", example = "worker01")
		private String userId;

		@NotBlank(message = "비밀번호는 필수입니다")
		@Schema(description = "비밀번호", example = "password123")
		private String password;

		public LoginReq(String userId, String password) {
			this.userId = userId;
			this.password = password;
		}

	}

	@Getter
	@NoArgsConstructor
	@Schema(description = "리프레시 토큰 요청")
	public static class RefreshTokenReq  {

		@Schema(description = "JWT 리프레시 토큰")
		private String refreshToken;

		public RefreshTokenReq(String refreshToken) {
			this.refreshToken = refreshToken;
		}
	}

	@Getter
	@NoArgsConstructor
	@Schema(description = "토큰 응답")
	public static class TokenRes {

		@Schema(description = "JWT 액세스 토큰")
		private String accessToken;

		@Schema(description = "JWT 리프레시 토큰")
		private String refreshToken;

		public TokenRes(String accessToken, String refreshToken) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}
	}
}
