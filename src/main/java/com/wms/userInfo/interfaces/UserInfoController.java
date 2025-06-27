package com.wms.userInfo.interfaces;

import java.util.List;
import java.util.Map;

import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "UserInfo Management", description = "APIs for managing UserInfo")
public class UserInfoController {

	private final UserInfoService userInfoService;
	private final DomainCacheManager<Long, String> userInfoCacheManager;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a new user", description = "Registers a new user")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "User created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public UserInfoDTO.Res createUser(@Valid @RequestBody UserInfoDTO.CreateReq request) {
		UserInfo saved = userInfoService.createUser(request);
		return new UserInfoDTO.Res(saved);
	}

	@GetMapping("/{userId}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get user by user id, not user idx")
	public UserInfoDTO.Res getUserByUserId(@PathVariable String userId) {
		UserInfo user = userInfoService.getUserByUserName(userId);
		return new UserInfoDTO.Res(user);
	}

	@GetMapping
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get users")
	public List<UserInfoDTO.Res> getUsers() {
		return userInfoService.getUsers().stream()
				.map(UserInfoDTO.Res::new)
				.toList();
	}

	@GetMapping("/type/{type}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Get users by type", description = "Retrieves all users of a specific type(WORKER, ADMIN)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
			@ApiResponse(responseCode = "400", description = "Invalid user type")
	})
	public List<UserInfoDTO.Res> getUserByType(@PathVariable UserType type) {
		return userInfoService.getUsersByType(type).stream()
				.map(UserInfoDTO.Res::new)
				.toList();
	}

	@PutMapping("/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Update user info")
	public UserInfoDTO.Res updateUser(@PathVariable Long id, @Valid @RequestBody UserInfoDTO.UpdateReq request) {
		UserInfo updated = userInfoService.updateUser(id, request);
		return new UserInfoDTO.Res(updated);
	}

	@PatchMapping("/{id}/change-password")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(summary = "Change user password")
	public void changePassword(@PathVariable Long id, @Valid @RequestBody UserInfoDTO.ChangePasswordReq request) {
		userInfoService.changePassword(id, request.getOldPassword(), request.getNewPassword());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete user")
	public void deleteUser(@PathVariable Long id) {
		userInfoService.deleteUser(id);
	}

	@GetMapping("/id_name_pair")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get user ID-name pairs", description = "Retrieves a map of user IDs to user names for reference")
	@ApiResponse(responseCode = "200", description = "Successfully retrieved user ID-name pairs")
	public Map<Long, String> getIdNamePair() {
		return userInfoCacheManager.getIdNamePair();
	}
}
