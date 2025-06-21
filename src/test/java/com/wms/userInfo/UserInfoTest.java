package com.wms.userInfo;

import com.wms.userInfo.domain.exception.UserInfoException;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사용자 정보 도메인 테스트")
class UserInfoTest {

    @Test
    @DisplayName("유효하지 않은 사용자명이면 예외가 발생한다")
    void testInvalidUsername() {
        assertThatThrownBy(() -> {
            UserInfo.builder()
                    .username("Invalid-Username")
                    .name("테스트 사용자")
                    .email("test@example.com")
                    .password(Password.builder().value("password123").build())
                    .type(UserType.WORKER)
                    .build();
        }).isInstanceOf(UserInfoException.ValidationEx.class);
    }

    @Test
    @DisplayName("유효한 사용자 정보로 사용자를 생성할 수 있다")
    void testValidUserCreation() {
        UserInfo user = UserInfo.builder()
                .username("test_user")
                .name("테스트 사용자")
                .email("test@example.com")
                .password(Password.builder().value("password123").build())
                .type(UserType.WORKER)
                .build();

        assertThat(user.getUsername()).isEqualTo("test_user");
        assertThat(user.getName()).isEqualTo("테스트 사용자");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getType()).isEqualTo(UserType.WORKER);
    }

    @Test
    @DisplayName("사용자 정보를 업데이트할 수 있다")
    void testUserUpdate() {
        UserInfo user = UserInfo.builder()
                .username("test_user")
                .name("테스트 사용자")
                .email("test@example.com")
                .password(Password.builder().value("password123").build())
                .type(UserType.WORKER)
                .build();

        user.update("업데이트된 사용자", "updated@example.com");

        assertThat(user.getName()).isEqualTo("업데이트된 사용자");
        assertThat(user.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @DisplayName("비밀번호를 변경할 수 있다")
    void testPasswordChange() {
        UserInfo user = UserInfo.builder()
                .username("test_user")
                .name("테스트 사용자")
                .email("test@example.com")
                .password(Password.builder().value("password123").build())
                .type(UserType.WORKER)
                .build();

        Password newPassword = user.getPassword().changePassword("newpassword123", "password123");
        assertThat(newPassword).isNotNull();
    }

    @Test
    @DisplayName("잘못된 기존 비밀번호로 변경하면 예외가 발생한다")
    void testWrongPasswordChange() {
        UserInfo user = UserInfo.builder()
                .username("test_user")
                .name("테스트 사용자")
                .email("test@example.com")
                .password(Password.builder().value("password123").build())
                .type(UserType.WORKER)
                .build();

        assertThatThrownBy(() -> {
            user.getPassword().changePassword("newpassword123", "wrongpassword");
        }).isInstanceOf(UserInfoException.WrongPasswordException.class);
    }
} 