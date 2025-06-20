package com.wms.userInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.applicationInfra.config.TestSecurityConfig;
import com.wms.applicationInfra.idnameMapCashing.DomainCacheManager;
import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.dto.UserInfoDTO;
import com.wms.userInfo.interfaces.UserInfoController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserInfoController.class)
@Import(TestSecurityConfig.class)
class UserInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserInfoService userInfoService;
    @MockBean
    private DomainCacheManager<Long, String> domainCacheManager;

    @Test
    @DisplayName("POST /users - 사용자 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createUser_Success() throws Exception {
        UserInfoDTO.CreateReq createReq = UserInfoDTO.CreateReq.builder()
                .userId("newuser")
                .name("New User")
                .email("new@test.com")
                .password("password")
                .type(UserType.WORKER)
                .build();
        UserInfo mockUser = createReq.toEntity();
        given(userInfoService.createUser(any(UserInfoDTO.CreateReq.class))).willReturn(mockUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("newuser"));
    }

    @Test
    @DisplayName("GET /users/{userId} - user ID로 사용자 조회")
    @WithMockUser
    void getUserByUsername_Success() throws Exception {
        UserInfo mockUser = UserInfo.builder().username("test").name("testName").email("t@t.com").password(new Password("password")).type(UserType.WORKER).build();
        given(userInfoService.getUserByUserName("test")).willReturn(mockUser);

        mockMvc.perform(get("/users/{userId}", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("test"))
                .andExpect(jsonPath("$.name").value("testName"));
    }

    @Test
    @DisplayName("PUT /users/{id} - 사용자 정보 수정")
    @WithMockUser
    void updateUser_Success() throws Exception {
        UserInfoDTO.UpdateReq updateReq = UserInfoDTO.UpdateReq.builder().name("Updated").email("up@date.com").build();
        UserInfo mockUser = UserInfo.builder().username("test").name("Updated").email("up@date.com").password(new Password("password")).type(UserType.WORKER).build();

        given(userInfoService.updateUser(eq(1L), any(UserInfoDTO.UpdateReq.class))).willReturn(mockUser);

        mockMvc.perform(put("/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @DisplayName("PATCH /users/{id}/change-password - 비밀번호 변경")
    @WithMockUser
    void changePassword_Success() throws Exception {
        UserInfoDTO.ChangePasswordReq req = new UserInfoDTO.ChangePasswordReq("oldpw", "newpw");
        doNothing().when(userInfoService).changePassword(eq(1L), eq("oldpw"), eq("newpw"));

        mockMvc.perform(patch("/users/{id}/change-password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /users/{id} - 사용자 삭제")
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        doNothing().when(userInfoService).deleteUser(1L);

        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isNoContent());
    }

} 