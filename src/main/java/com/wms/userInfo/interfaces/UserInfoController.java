package com.wms.userInfo.interfaces;

import java.util.List;
import java.util.Map;

import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "사용자 관리", description = "사용자 정보 관리 API")
public class UserInfoController {

	private final UserInfoService userInfoService;
	private final DomainCacheManager<Long, String> userInfoCacheManager;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "새 사용자 생성", description = "새로운 사용자를 등록합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "사용자 생성 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
	})
	public UserInfoDTO.Res createUser(@Valid @RequestBody UserInfoDTO.CreateReq request) {
		UserInfo saved = userInfoService.createUser(request);
		return new UserInfoDTO.Res(saved);
	}

	@GetMapping("/{userId}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "사용자 ID로 조회", description = "사용자 ID(사용자명)로 사용자를 조회합니다 (사용자 idx가 아님)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "사용자 조회 성공"),
			@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	public UserInfoDTO.Res getUserByUserId(
			@Parameter(description = "사용자 ID(사용자명)") @PathVariable String userId) {
		UserInfo user = userInfoService.getUserByUserName(userId);
		return new UserInfoDTO.Res(user);
	}

	@GetMapping
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "사용자 목록 조회", description = "모든 사용자 목록을 조회합니다")
	@ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공")
	public List<UserInfoDTO.Res> getUsers() {
		return userInfoService.getUsers().stream()
				.map(UserInfoDTO.Res::new)
				.toList();
	}

	@GetMapping("/type/{type}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "타입별 사용자 조회", description = "특정 타입(작업자, 관리자)의 모든 사용자를 조회합니다")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "타입별 사용자 조회 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 사용자 타입")
	})
	public List<UserInfoDTO.Res> getUserByType(
			@Parameter(description = "사용자 타입") @PathVariable UserType type) {
		return userInfoService.getUsersByType(type).stream()
				.map(UserInfoDTO.Res::new)
				.toList();
	}

	@PutMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "사용자 정보 수정", description = "기존 사용자의 정보를 수정합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	public UserInfoDTO.Res updateUser(
			@Parameter(description = "사용자 ID") @PathVariable Long id, 
			@Valid @RequestBody UserInfoDTO.UpdateReq request) {
		UserInfo updated = userInfoService.updateUser(id, request);
		return new UserInfoDTO.Res(updated);
	}

	@PatchMapping("/{id}/change-password")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "사용자 비밀번호 변경", description = "사용자의 비밀번호를 변경합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 비밀번호"),
			@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	public void changePassword(
			@Parameter(description = "사용자 ID") @PathVariable Long id, 
			@Valid @RequestBody UserInfoDTO.ChangePasswordReq request) {
		userInfoService.changePassword(id, request.getOldPassword(), request.getNewPassword());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "사용자 삭제", description = "기존 사용자를 삭제합니다")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "사용자 삭제 성공"),
			@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	public void deleteUser(@Parameter(description = "사용자 ID") @PathVariable Long id) {
		userInfoService.deleteUser(id);
	}

	@GetMapping("/id_name_pair")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "사용자 ID-이름 쌍 조회", description = "참조용 사용자 ID와 이름의 매핑 정보를 조회합니다")
	@ApiResponse(responseCode = "200", description = "사용자 ID-이름 쌍 조회 성공")
	public Map<Long, String> getIdNamePair() {
		return userInfoCacheManager.getIdNamePair();
	}
}
