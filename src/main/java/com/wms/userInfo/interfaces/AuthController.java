package com.wms.userInfo.interfaces;

import com.wms.userInfo.application.AuthService;
import com.wms.userInfo.dto.AuthDTO;
import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "auth Management", description = "APIs for managing auth")
public class AuthController {

	private final AuthService authService;
	private final UserInfoService userInfoService;

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@ApiResponse(responseCode = "200", description = "Login success")
	@ApiResponse(responseCode = "401", description = "Invalid credentials")
	@ApiResponse(responseCode = "403", description = "User not allowed to login")
	public AuthDTO.TokenRes login(@Valid @RequestBody AuthDTO.LoginReq request) {
		return authService.login(request);
	}

	@PostMapping("/logout")
	@Operation(summary = "User logout")
	public ResponseEntity<Void> logout(@RequestBody @Validated AuthDTO.RefreshTokenReq request) {
		authService.logout(request.getRefreshToken());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/refresh-token")
	@Operation(summary = "Refresh JWT tokens")
	public ResponseEntity<AuthDTO.TokenRes> refreshToken(@RequestBody @Validated AuthDTO.RefreshTokenReq request) {
		AuthDTO.TokenRes response = authService.refreshToken(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/join")
	@ResponseStatus(HttpStatus.CREATED)
	public UserInfoDTO.Res join(@Valid @RequestBody UserInfoDTO.JoinReq request) {
		UserInfo user = userInfoService.join(request);
		return new UserInfoDTO.Res(user);
	}

	@DeleteMapping("/withdraw/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@PathVariable Long id, @AuthenticationPrincipal UserInfo currentUser) {
		userInfoService.withdraw(id, currentUser);
	}
}

