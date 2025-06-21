package com.wms.userInfo.domain.model;

import java.util.regex.Pattern;

import com.wms.applicationInfra.domain.BaseEntity;
import com.wms.userInfo.domain.exception.UserInfoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo extends BaseEntity {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

	@Column(name = "username", nullable = false, unique = true, length = 20)
	private String username; // 로그인 ID

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Embedded
	private Password password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserType type;

	@Builder
	public UserInfo(String username, String name, String email, Password password, UserType type) {
		this.username = username;
		this.name = name;
		this.email = email;
		this.password = password;
		this.type = type;
		validateUsername();
	}

	public void validateUsername() {
		if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
			throw UserInfoException.validation(String.format("유효하지 않은 사용자명입니다: %s (소문자, 숫자, 언더스코어만 사용 가능)", username));
		}
	}

	public void update(String name, String email) {
		this.name = name;
		this.email = email;
	}

	public void changePassword(String newPassword, String oldPassword) {
		this.password = this.password.changePassword(newPassword, oldPassword);
	}

	public boolean isAdmin() {
		return this.type == UserType.ADMIN;
	}

	public boolean isWorker() {
		return this.type == UserType.WORKER;
	}

	// Worker 관련 메서드
	public boolean canAccessWorkerDashboard() {
		return isWorker();
	}

	public boolean canAccessAdminDashboard() {
		return isAdmin();
	}
}
