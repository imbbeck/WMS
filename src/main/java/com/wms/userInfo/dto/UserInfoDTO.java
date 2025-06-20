package com.wms.user.dto;

import com.wms.user.domain.model.Password;
import com.wms.user.domain.model.UserInfo;
import com.wms.user.domain.model.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserInfoDTO {

	public static final int PASSWORD_MIN_LENGTH = 4;

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "User creation request")
	public static class CreateReq {

		@NotBlank(message = "UserId is required")
		@Pattern(regexp = "^[a-z0-9_]+$", message = "Username must contain only lowercase letters, numbers, and underscore")
		@Size(min = 3, max = 20, message = "Username length must be between 3 and 20")
		@Schema(description = "userId", example = "admin_1")
		private String userId;

		@NotBlank(message = "Name is required")
		@Schema(description = "User's real name", example = "정도영")
		private String name;

		@NotBlank(message = "Email is required")
		@Email(message = "Email format must be valid")
		private String email;

		@NotBlank(message = "Password is required")
		@Size(min = PASSWORD_MIN_LENGTH, message = "Password must be at least " + PASSWORD_MIN_LENGTH + " characters")
		private String password;

		@NotNull(message = "User type is required")
		@Schema(description = "User type", example = "ADMIN, WORKER")
		private UserType type;

		@Builder
		public CreateReq(String userId, String name, String email, String password, UserType type) {
			this.userId = userId;
			this.name = name;
			this.email = email;
			this.password = password;
			this.type = type;
		}

		public UserInfo toEntity() {
			return UserInfo.builder()
					.username(userId)
					.name(name)
					.email(email)
					.password(new Password(password)) // Password 엔티티 생성자에 평문 비밀번호 전달
					.type(type)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "User update request")
	public static class UpdateReq {

		@NotBlank(message = "Name is required")
		@Schema(description = "User's real name", example = "홍길동")
		private String name;

		@NotBlank(message = "Email is required")
		@Email(message = "Email format must be valid")
		@Schema(description = "User email", example = "user01@example.com")
		private String email;

		@Builder
		public UpdateReq(String name, String email) {
			this.name = name;
			this.email = email;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "User response")
	public static class Res {

		@Schema(description = "idx", example = "1")
		private Long id;

		@Schema(description = "user Id", example = "user01")
		private String userId;

		@Schema(description = "User's real name", example = "정도영")
		private String name;

		@Schema(description = "User email", example = "user01@example.com")
		private String email;

		@Schema(description = "User type", example = "ADMIN")
		private UserType type;

		public Res(UserInfo userInfo) {
			this.id = userInfo.getId();
			this.userId = userInfo.getUsername();
			this.name = userInfo.getName();
			this.email = userInfo.getEmail();
			this.type = userInfo.getType();
		}

		@Builder
		public Res(Long id, String username, String name, String email, UserType type) {
			this.id = id;
			this.userId = username;
			this.name = name;
			this.email = email;
			this.type = type;
		}
	}
}
