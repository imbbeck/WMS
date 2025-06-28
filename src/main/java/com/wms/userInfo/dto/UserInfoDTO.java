package com.wms.userInfo.dto;

import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
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
	@Schema(description = "사용자 생성 요청")
	public static class CreateReq {

		@NotBlank(message = "사용자 ID는 필수입니다")
		@Pattern(regexp = "^[a-z0-9_]+$", message = "사용자 ID는 소문자, 숫자, 언더스코어만 사용할 수 있습니다")
		@Size(min = 3, max = 20, message = "사용자 ID는 3~20자여야 합니다")
		@Schema(description = "사용자 ID", example = "admin_1")
		private String userId;

		@NotBlank(message = "이름은 필수입니다")
		@Schema(description = "사용자 실명", example = "정도영")
		private String name;

		@NotBlank(message = "이메일은 필수입니다")
		@Email(message = "이메일 형식이 올바르지 않습니다")
		@Schema(description = "사용자 이메일", example = "admin@example.com")
		private String email;

		@NotBlank(message = "비밀번호는 필수입니다")
		@Size(min = PASSWORD_MIN_LENGTH, message = "비밀번호는 최소 " + PASSWORD_MIN_LENGTH + "자 이상이어야 합니다")
		@Schema(description = "비밀번호", example = "password123")
		private String password;

		@NotNull(message = "사용자 타입은 필수입니다")
		@Schema(description = "사용자 타입", example = "ADMIN", allowableValues = {"ADMIN", "WORKER"})
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
	@Schema(description = "회원가입 요청")
	public static class JoinReq {

		@NotBlank(message = "사용자 ID는 필수입니다")
		@Pattern(regexp = "^[a-z0-9_]+$", message = "사용자 ID는 소문자, 숫자, 언더스코어만 사용할 수 있습니다")
		@Size(min = 3, max = 20, message = "사용자 ID는 3~20자여야 합니다")
		@Schema(description = "사용자 ID", example = "worker_1")
		private String userId;

		@NotBlank(message = "이름은 필수입니다")
		@Schema(description = "사용자 실명", example = "정도영")
		private String name;

		@NotBlank(message = "이메일은 필수입니다")
		@Email(message = "이메일 형식이 올바르지 않습니다")
		@Schema(description = "사용자 이메일", example = "worker@example.com")
		private String email;

		@NotBlank(message = "비밀번호는 필수입니다")
		@Size(min = PASSWORD_MIN_LENGTH, message = "비밀번호는 최소 " + PASSWORD_MIN_LENGTH + "자 이상이어야 합니다")
		@Schema(description = "비밀번호", example = "password123")
		private String password;

		@Builder
		public JoinReq(String userId, String name, String email, String password) {
			this.userId = userId;
			this.name = name;
			this.email = email;
			this.password = password;
		}

		public UserInfo toEntity() {
			return UserInfo.builder()
					.username(userId)
					.name(name)
					.email(email)
					.password(new Password(password)) // Password 엔티티 생성자에 평문 비밀번호 전달
					.type(UserType.WORKER)
					.build();
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "사용자 수정 요청")
	public static class UpdateReq {

		@NotBlank(message = "이름은 필수입니다")
		@Schema(description = "사용자 실명", example = "홍길동")
		private String name;

		@NotBlank(message = "이메일은 필수입니다")
		@Email(message = "이메일 형식이 올바르지 않습니다")
		@Schema(description = "사용자 이메일", example = "user01@example.com")
		private String email;

		@Builder
		public UpdateReq(String name, String email) {
			this.name = name;
			this.email = email;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "비밀번호 변경 요청")
	public static class ChangePasswordReq {

		@NotBlank(message = "기존 비밀번호는 필수입니다")
		@Schema(description = "기존 비밀번호", example = "oldPassword123")
		private String oldPassword;

		@NotBlank(message = "새 비밀번호는 필수입니다")
		@Size(min = PASSWORD_MIN_LENGTH, message = "비밀번호는 최소 " + PASSWORD_MIN_LENGTH + "자 이상이어야 합니다")
		@Schema(description = "새 비밀번호", example = "newPassword123")
		private String newPassword;

		@Builder
		public ChangePasswordReq(String oldPassword, String newPassword) {
			this.oldPassword = oldPassword;
			this.newPassword = newPassword;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "사용자 응답")
	public static class Res {

		@Schema(description = "사용자 고유 번호", example = "1")
		private Long id;

		@Schema(description = "사용자 ID", example = "user01")
		private String userId;

		@Schema(description = "사용자 실명", example = "정도영")
		private String name;

		@Schema(description = "사용자 이메일", example = "user01@example.com")
		private String email;

		@Schema(description = "사용자 타입", example = "ADMIN")
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
