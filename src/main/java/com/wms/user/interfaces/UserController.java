package com.wms.user.interfaces;

import com.wms.user.application.UserService;
import com.wms.user.dto.UserRequest;
import com.wms.user.dto.UserResponse;
import com.wms.user.mapper.UserMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userMapper.toResponse(userService.createUser(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userMapper.toResponse(userService.getUser(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userMapper.toResponse(userService.getUserByEmail(email)));
    }
} 