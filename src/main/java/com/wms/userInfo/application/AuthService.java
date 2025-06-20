package com.wms.auth.application;

import com.wms.auth.dto.AuthDTO;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final UserInfoRepository userInfoRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider; // JWT 발급/검증 유틸 클래스 (아래 별도 구현 예정)

	/**
	 * 로그인 처리
	 * WORKER 타입만 로그인 가능
	 */
	@Transactional
	public AuthDTO.LoginRes login(AuthDTO.LoginReq request) {
		UserInfo user = userInfoRepository.findByUsername(request.getUserId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid userId or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword().getValue())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid userId or password");
		}

		// JWT 토큰 발급
		String accessToken = jwtProvider.generateAccessToken(user);
		String refreshToken = jwtProvider.generateRefreshToken(user);

		return new AuthDTO.LoginRes(accessToken, refreshToken);
	}

	@Transactional
	public UserInfo join(UserInfoDTO.JoinReq request) {
		
		UserInfo user = request.toEntity();
		return userInfoRepository.save(user);
	}

	@Transactional
	public void withdraw(Long userId, UserInfo currentUser) {
		if (currentUser.getType() != UserType.WORKER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only WORKER users can withdraw");
		}

		UserInfo user = userInfoRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		userInfoRepository.delete(user);
	}

	// 토큰 재발급, 로그아웃 등은 필요하면 추가
}

