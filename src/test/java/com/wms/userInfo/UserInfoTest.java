package com.wms.userInfo;

import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserInfoTest {
    @Test
    @DisplayName("유효하지 않은 유저이름(username)으로 생성 시 예외 발생")
    void create_WithInvalidUsername_ThrowsException() {
        assertThatThrownBy(() -> UserInfo.builder()
                .username("Invalid Username")
                .name("test")
                .email("test@test.com")
                .password(new Password("password"))
                .type(UserType.ADMIN)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username must contain only lowercase letters, numbers, and underscores.");
    }

    @Test
    @DisplayName("비밀번호 변경 시 기존 비밀번호가 틀리면 예외 발생")
    void changePassword_WithWrongOldPassword_ThrowsException() {
        UserInfo user = UserInfo.builder()
                .username("test_user")
                .name("test")
                .email("test@test.com")
                .password(new Password("old_password"))
                .type(UserType.ADMIN)
                .build();

        assertThatThrownBy(() -> user.changePassword("new_password", "wrong_old_password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기존 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호를 성공적으로 변경한다.")
    void changePassword_Success() {
        Password initialPassword = new Password("old_password");
        UserInfo user = UserInfo.builder()
                .username("test_user")
                .name("test")
                .email("test@test.com")
                .password(initialPassword)
                .type(UserType.ADMIN)
                .build();
        user.changePassword("new_password", "old_password");
        assertThat(user.getPassword().isMatched("new_password")).isTrue();
    }

    @Test
    @DisplayName("유저 정보를 수정한다.")
    void update_Success() {
        UserInfo user = UserInfo.builder()
                .username("test_user")
                .name("old_name")
                .email("old@email.com")
                .password(new Password("password"))
                .type(UserType.WORKER)
                .build();
        user.update("new_name", "new@email.com");
        assertThat(user.getName()).isEqualTo("new_name");
        assertThat(user.getEmail()).isEqualTo("new@email.com");
    }
} 