package com.wms.auth.interfaces;

import com.wms.auth.application.AuthService;
import com.wms.auth.dto.AuthDTO;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "auth Management", description = "APIs for managing auth")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@ApiResponse(responseCode = "200", description = "Login success")
	@ApiResponse(responseCode = "401", description = "Invalid credentials")
	@ApiResponse(responseCode = "403", description = "User not allowed to login")
	public AuthDTO.LoginRes login(@Valid @RequestBody AuthDTO.LoginReq request) {
		return authService.login(request);
	}

	@PostMapping("/join")
	@ResponseStatus(HttpStatus.CREATED)
	public UserInfoDTO.Res join(@Valid @RequestBody UserInfoDTO.JoinReq request) {
		UserInfo user = authService.join(request);
		return new UserInfoDTO.Res(user);
	}

	@DeleteMapping("/withdraw/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@PathVariable Long id, @AuthenticationPrincipal UserInfo currentUser) {
		authService.withdraw(id, currentUser);
	}
}

