//package com.wms.user.dto;
//
//import com.wms.user.domain.model.UserType;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//
//@Getter
//public class UserRequest {
//
//    @NotBlank(message = "이름은 필수입니다")
//    private String name;
//
//    @NotBlank(message = "이메일은 필수입니다")
//    @Email(message = "올바른 이메일 형식이 아닙니다")
//    private String email;
//
//    @NotBlank(message = "비밀번호는 필수입니다")
//    private String password;
//
//    @NotNull(message = "사용자 타입은 필수입니다")
//    private UserType type;
//}