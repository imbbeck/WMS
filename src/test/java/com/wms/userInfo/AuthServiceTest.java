package com.wms.userInfo;

import com.wms.userInfo.application.AuthService;
import com.wms.userInfo.application.JwtProvider;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.userInfo.dto.AuthDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private JwtProvider jwtProvider;

    private UserInfo user;
    private final String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        user = UserInfo.builder()
                .username("testuser")
                .name("Test User")
                .email("test@test.com")
                .password(new Password(rawPassword))
                .type(UserType.WORKER)
                .build();
        userInfoRepository.save(user);
    }

    @Test
    @DisplayName("로그인 성공 시 AccessToken과 RefreshToken을 발급한다.")
    void login_Success() {
        // given
        AuthDTO.LoginReq req = new AuthDTO.LoginReq("testuser", rawPassword);

        // when
        AuthDTO.TokenRes tokens = authService.login(req);

        // then
        assertThat(tokens.getAccessToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(jwtProvider.validateToken(tokens.getAccessToken())).isTrue();
        assertThat(jwtProvider.getUsernameFromToken(tokens.getAccessToken())).isEqualTo("testuser");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 로그인 시 예외가 발생한다.")
    void login_WithWrongId_ThrowsException() {
        // given
        AuthDTO.LoginReq req = new AuthDTO.LoginReq("wronguser", rawPassword);

        // when & then
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid userId or password");
    }

    @Test
    @DisplayName("틀린 비밀번호로 로그인 시 예외가 발생한다.")
    void login_WithWrongPassword_ThrowsException() {
        // given
        AuthDTO.LoginReq req = new AuthDTO.LoginReq("testuser", "wrong_password");

        // when & then
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid userId or password");
    }

    @Test
    @DisplayName("유효한 RefreshToken으로 토큰을 재발급한다.")
    void refreshToken_Success() {
        // given
        String refreshToken = jwtProvider.generateRefreshToken(user);
        AuthDTO.RefreshTokenReq req = new AuthDTO.RefreshTokenReq(refreshToken);

        // when
        AuthDTO.TokenRes newTokens = authService.refreshToken(req);

        // then
        assertThat(newTokens.getAccessToken()).isNotNull();
        assertThat(newTokens.getRefreshToken()).isNotNull();
        assertThat(jwtProvider.validateToken(newTokens.getAccessToken())).isTrue();
        assertThat(jwtProvider.getUsernameFromToken(newTokens.getAccessToken())).isEqualTo("testuser");
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken으로 재발급 요청 시 예외가 발생한다.")
    void refreshToken_WithInvalidToken_ThrowsException() {
        // given
        AuthDTO.RefreshTokenReq req = new AuthDTO.RefreshTokenReq("invalid.refresh.token");

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid refresh token");
    }
} 