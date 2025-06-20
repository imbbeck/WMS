package com.wms.userInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.userInfo.application.AuthService;
import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.dto.AuthDTO;
import com.wms.userInfo.dto.UserInfoDTO;
import com.wms.userInfo.interfaces.AuthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private UserInfoService userInfoService;

    @Test
    @DisplayName("POST /auth/login - 로그인 성공")
    void login_Success() throws Exception {
        // given
        AuthDTO.LoginReq loginReq = new AuthDTO.LoginReq("testuser", "password");
        AuthDTO.TokenRes tokenRes = new AuthDTO.TokenRes("access_token", "refresh_token");
        given(authService.login(any(AuthDTO.LoginReq.class))).willReturn(tokenRes);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"));
    }

    @Test
    @DisplayName("POST /auth/join - 회원가입 성공")
    void join_Success() throws Exception {
        // given
        UserInfoDTO.JoinReq joinReq = UserInfoDTO.JoinReq.builder()
                .userId("newbie")
                .name("New User")
                .email("new@test.com")
                .password("password123")
                .build();
        UserInfo mockUser = joinReq.toEntity();
        given(userInfoService.join(any(UserInfoDTO.JoinReq.class))).willReturn(mockUser);

        // when & then
        mockMvc.perform(post("/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("newbie"));
    }

    @Test
    @DisplayName("POST /auth/refresh-token - 토큰 재발급 성공")
    void refreshToken_Success() throws Exception {
        // given
        AuthDTO.RefreshTokenReq refreshReq = new AuthDTO.RefreshTokenReq("old_refresh_token");
        AuthDTO.TokenRes tokenRes = new AuthDTO.TokenRes("new_access_token", "new_refresh_token");
        given(authService.refreshToken(any(AuthDTO.RefreshTokenReq.class))).willReturn(tokenRes);

        // when & then
        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new_access_token"));
    }
} 