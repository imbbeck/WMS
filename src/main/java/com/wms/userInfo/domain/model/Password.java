package com.wms.user.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Password {

	private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

	@Column(name = "password", nullable = false)
	private String value;

	@Builder
	public Password(final String value) {
		this.value = encodePassword(value);
	}

	public boolean isMatched(final String rawPassword) {
		return isMatches(rawPassword);
	}

	public Password changePassword(final String newPassword, final String oldPassword) {
		if (!isMatched(oldPassword)) {
			throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
		}
		// 내부 상태 변경 없이 새 객체 반환
		return new Password(newPassword);
	}

	private String encodePassword(final String password) {
		return ENCODER.encode(password);
	}

	private boolean isMatches(String rawPassword) {
		return ENCODER.matches(rawPassword, this.value);
	}
}
