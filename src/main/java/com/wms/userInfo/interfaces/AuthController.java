package com.wms.userInfo.interfaces;

import com.wms.userInfo.application.AuthService;
import com.wms.userInfo.dto.AuthDTO;
import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증 관리", description = "인증 및 권한 관리 API")
public class AuthController {

	private final AuthService authService;
	private final UserInfoService userInfoService;

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "사용자 로그인", description = "사용자 인증 정보로 로그인합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그인 성공"),
			@ApiResponse(responseCode = "401", description = "인증 정보가 올바르지 않음"),
			@ApiResponse(responseCode = "403", description = "로그인이 허용되지 않는 사용자")
	})
	public AuthDTO.TokenRes login(@Valid @RequestBody AuthDTO.LoginReq request) {
		return authService.login(request);
	}

	@PostMapping("/logout")
	@Operation(summary = "사용자 로그아웃", description = "현재 사용자를 로그아웃 처리합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 리프레시 토큰")
	})
	public ResponseEntity<Void> logout(@RequestBody @Validated AuthDTO.RefreshTokenReq request) {
		authService.logout(request.getRefreshToken());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/refresh-token")
	@Operation(summary = "JWT 토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 JWT 토큰을 발급합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
			@ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
	})
	public ResponseEntity<AuthDTO.TokenRes> refreshToken(@RequestBody @Validated AuthDTO.RefreshTokenReq request) {
		AuthDTO.TokenRes response = authService.refreshToken(request);
		return ResponseEntity.ok(response);
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
	@Operation(summary = "회원탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "회원탈퇴 성공"),
			@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
	})
	public void withdraw(@AuthenticationPrincipal UserInfo currentUser) {
		userInfoService.withdraw(currentUser);
	}
}
