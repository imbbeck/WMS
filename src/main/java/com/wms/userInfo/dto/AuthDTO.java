package com.wms.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDTO {

	@Getter
	@NoArgsConstructor
	@Schema(description = "Login request")
	public static class LoginReq {

		@NotBlank
		@Schema(description = "UserId", example = "worker01")
		private String userId;

		@NotBlank
		@Schema(description = "Password", example = "password123")
		private String password;

		public LoginReq(String userId, String password) {
			this.userId = userId;
			this.password = password;
		}
	}

	@Getter
	@NoArgsConstructor
	@Schema(description = "Login response")
	public static class LoginRes {

		@Schema(description = "JWT access token")
		private String accessToken;

		@Schema(description = "JWT refresh token")
		private String refreshToken;

		public LoginRes(String accessToken, String refreshToken) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}
	}
}

