package com.wms.userInfo.interfaces;

import com.wms.userInfo.application.AuthService;
import com.wms.userInfo.dto.AuthDTO;
import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증 관리", description = "인증 및 권한 관리 API")
public class AuthController {

	private final AuthService authService;
	private final UserInfoService userInfoService;
	
	@Value("${spring.profiles.active:dev}")
	private String activeProfile;

	// 쿠키 설정 상수
	private static final String ACCESS_TOKEN_COOKIE = "accessToken";
	private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
	private static final int ACCESS_TOKEN_EXPIRE = 15 * 60; // 15분 (초)
	private static final int REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60; // 7일 (초)

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "사용자 로그인", description = "사용자 인증 정보로 로그인합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그인 성공"),
			@ApiResponse(responseCode = "401", description = "인증 정보가 올바르지 않음"),
			@ApiResponse(responseCode = "403", description = "로그인이 허용되지 않는 사용자")
	})
	public AuthDTO.TokenRes login(@Valid @RequestBody AuthDTO.LoginReq request, HttpServletResponse response) {
		AuthDTO.TokenRes tokenResponse = authService.login(request);

		// 쿠키에 토큰 설정
		setTokenCookies(response, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
		
		return tokenResponse;
	}

	@PostMapping("/logout")
	@SecurityRequirement(name = "bearerAuth")
	@SecurityRequirement(name = "cookieAuth")
	@Operation(summary = "사용자 로그아웃", description = "현재 사용자를 로그아웃 처리합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 리프레시 토큰")
	})
	public ResponseEntity<Void> logout(@RequestBody @Validated AuthDTO.RefreshTokenReq request, HttpServletResponse response) {
		authService.logout(request.getRefreshToken());
		
		// 쿠키에서 토큰 삭제
		clearTokenCookies(response);
		
		return ResponseEntity.ok().build();
	}

	@PostMapping("/logout-cookie")
	@SecurityRequirement(name = "cookieAuth")
	@Operation(summary = "쿠키 기반 로그아웃", description = "쿠키에서 리프레시 토큰을 읽어 로그아웃 처리합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 리프레시 토큰 또는 쿠키 없음")
	})
	public ResponseEntity<Void> logoutWithCookie(HttpServletRequest request, HttpServletResponse response) {
		// 쿠키에서 리프레시 토큰 추출
		String refreshToken = getTokenFromCookie(request, REFRESH_TOKEN_COOKIE);
		if (refreshToken == null || refreshToken.isEmpty()) {
			throw new IllegalArgumentException("리프레시 토큰이 없습니다");
		}
		
		authService.logout(refreshToken);
		
		// 쿠키에서 토큰 삭제
		clearTokenCookies(response);
		
		return ResponseEntity.ok().build();
	}

	@PostMapping("/refresh-token")
	@Operation(summary = "JWT 토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 JWT 토큰을 발급합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
			@ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
	})
	public ResponseEntity<AuthDTO.TokenRes> refreshToken(@RequestBody @Validated AuthDTO.RefreshTokenReq request, HttpServletResponse response) {
		AuthDTO.TokenRes tokenResponse = authService.refreshToken(request);
		// 쿠키에 새로운 토큰 설정
		setTokenCookies(response, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());

		
		return ResponseEntity.ok(tokenResponse);
	}

	@PostMapping("/refresh-token-cookie")
	@SecurityRequirement(name = "cookieAuth")
	@Operation(summary = "쿠키 기반 JWT 토큰 갱신", description = "쿠키에서 리프레시 토큰을 읽어 새로운 JWT 토큰을 발급합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
			@ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰 또는 쿠키 없음")
	})
	public ResponseEntity<AuthDTO.TokenRes> refreshTokenWithCookie(HttpServletRequest request, HttpServletResponse response) {
		// 쿠키에서 리프레시 토큰 추출
		String refreshToken = getTokenFromCookie(request, REFRESH_TOKEN_COOKIE);
		if (refreshToken == null || refreshToken.isEmpty()) {
			throw new IllegalArgumentException("리프레시 토큰이 없습니다");
		}
		
		// 리프레시 토큰으로 새 토큰 발급
		AuthDTO.RefreshTokenReq refreshRequest = new AuthDTO.RefreshTokenReq(refreshToken);
		AuthDTO.TokenRes tokenResponse = authService.refreshToken(refreshRequest);
		
		// 쿠키에 새로운 토큰 설정
		setTokenCookies(response, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
		
		return ResponseEntity.ok(tokenResponse);
	}

	@PostMapping("/join")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "회원가입 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "409", description = "이미 존재하는 사용자명 또는 이메일")
	})
	public UserInfoDTO.Res join(@Valid @RequestBody UserInfoDTO.JoinReq request) {
		UserInfo user = userInfoService.join(request);
		return new UserInfoDTO.Res(user);
	}

	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/withdraw")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirement(name = "bearerAuth")
	@SecurityRequirement(name = "cookieAuth")
	@Operation(summary = "회원탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "회원탈퇴 성공"),
			@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
	})
	public void withdraw(@AuthenticationPrincipal UserInfo currentUser) {
		userInfoService.withdraw(currentUser);
	}

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "bearerAuth")
	@SecurityRequirement(name = "cookieAuth")
	@Operation(summary = "현재 로그인 사용자 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
	})
	public ResponseEntity<UserInfoDTO.Res> getCurrentUser(@AuthenticationPrincipal UserInfo currentUser) {
		return ResponseEntity.ok(new UserInfoDTO.Res(currentUser));
	}

	/**
	 * 토큰을 쿠키에 설정하는 메서드
	 */
	private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
		// Access Token 쿠키 설정
		Cookie accessTokenCookie = createSecureCookie(ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_EXPIRE);
		response.addCookie(accessTokenCookie);
		
		// Refresh Token 쿠키 설정 (HttpOnly, Secure)
		Cookie refreshTokenCookie = createSecureCookie(REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_EXPIRE);
		response.addCookie(refreshTokenCookie);
	}

	/**
	 * 토큰 쿠키를 삭제하는 메서드
	 */
	private void clearTokenCookies(HttpServletResponse response) {
		// Access Token 쿠키 삭제
		Cookie accessTokenCookie = createCookie(ACCESS_TOKEN_COOKIE, "", 0);
		response.addCookie(accessTokenCookie);
		
		// Refresh Token 쿠키 삭제
		Cookie refreshTokenCookie = createSecureCookie(REFRESH_TOKEN_COOKIE, "", 0);
		response.addCookie(refreshTokenCookie);
	}

	/**
	 * 기본 쿠키를 생성하는 메서드
	 */
	private Cookie createCookie(String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		cookie.setSecure(!"dev".equals(activeProfile)); // 개발환경에서는 false, 운영환경에서는 true
		return cookie;
	}

	/**
	 * 보안 쿠키를 생성하는 메서드 (RefreshToken용)
	 */
	private Cookie createSecureCookie(String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		cookie.setHttpOnly(true); // JavaScript에서 접근 불가
//		cookie.setSecure(!"dev".equals(activeProfile)); // 개발환경에서는 false, 운영환경에서는 true
		cookie.setSecure(false);
		cookie.setAttribute("SameSite", "Lax"); // CSRF 보호
		return cookie;
	}

	/**
	 * 쿠키에서 토큰을 추출하는 메서드
	 */
	private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (cookieName.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
}
