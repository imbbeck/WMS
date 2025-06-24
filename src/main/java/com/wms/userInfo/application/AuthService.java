package com.wms.userInfo.application;

import com.wms.userInfo.domain.exception.AuthException;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.userInfo.dto.AuthDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final UserInfoRepository userInfoRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	@Transactional
	public AuthDTO.TokenRes login(AuthDTO.LoginReq request) {
		UserInfo user = userInfoRepository.findByUsername(request.getUserId())
				.orElseThrow(() -> AuthException.unauthorize("잘못된 사용자 ID 또는 비밀번호입니다"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword().getValue())) {
			throw AuthException.unauthorize("잘못된 사용자 ID 또는 비밀번호입니다");
		}

		String accessToken = jwtProvider.generateAccessToken(user);
		String refreshToken = jwtProvider.generateRefreshToken(user);

		return new AuthDTO.TokenRes(accessToken, refreshToken);
	}

	@Transactional
	public AuthDTO.TokenRes refreshToken(AuthDTO.RefreshTokenReq refreshToken) {
		if (!jwtProvider.validateToken(refreshToken.getRefreshToken())) {
			throw AuthException.unauthorize("유효하지 않은 리프레시 토큰입니다");
		}

		String username = jwtProvider.getUsernameFromToken(refreshToken.getRefreshToken());
		UserInfo user = userInfoRepository.findByUsername(username)
				.orElseThrow(() -> AuthException.unauthorize("유효하지 않은 리프레시 토큰입니다"));

		String newAccessToken = jwtProvider.generateAccessToken(user);
		String newRefreshToken = jwtProvider.generateRefreshToken(user);

		return new AuthDTO.TokenRes(newAccessToken, newRefreshToken);
	}

	@Transactional
	public void logout(String refreshToken) {
		// 토큰 블랙리스트 저장 로직 필요 (예: Redis)
		// 또는 클라이언트에서 토큰 폐기 후 서버에서 별도 관리 안함
		// 간단히 validate만 해서 예외처리
		if (!jwtProvider.validateToken(refreshToken)) {
			throw AuthException.unauthorize("유효하지 않은 리프레시 토큰입니다");
		}

		// TODO: Redis 등에 블랙리스트 저장 처리

		// 실제 구현 시 클라이언트에게서 토큰 삭제 권고
	}
}
