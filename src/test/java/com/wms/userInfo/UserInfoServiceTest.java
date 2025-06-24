package com.wms.userInfo;

import com.wms.userInfo.application.UserInfoService;
import com.wms.userInfo.domain.exception.UserInfoException;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.userInfo.dto.UserInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserInfoServiceTest {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserInfoRepository userInfoRepository;

    private final String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        userInfoRepository.deleteAll(); // 이전 테스트 데이터 클리어
        UserInfoDTO.CreateReq adminReq = UserInfoDTO.CreateReq.builder()
                .userId("admin")
                .name("Admin")
                .email("admin@test.com")
                .password(rawPassword)
                .type(UserType.ADMIN)
                .build();
        userInfoService.createUser(adminReq);
    }

    @Test
    @DisplayName("새로운 사용자를 생성한다.")
    void createUser_Success() {
        // given
        UserInfoDTO.CreateReq workerReq = UserInfoDTO.CreateReq.builder()
                .userId("worker")
                .name("Worker")
                .email("worker@test.com")
                .password(rawPassword)
                .type(UserType.WORKER)
                .build();

        // when
        UserInfo savedUser = userInfoService.createUser(workerReq);

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("worker");
        assertThat(savedUser.getType()).isEqualTo(UserType.WORKER);
    }

    @Test
    @DisplayName("중복된 userId로 사용자 생성 시 예외 발생")
    void createUser_WithDuplicateUserId_ThrowsException() {
        // given
        UserInfoDTO.CreateReq duplicateReq = UserInfoDTO.CreateReq.builder()
                .userId("admin") // 이미 존재하는 ID
                .name("Another Admin")
                .email("another@test.com")
                .password(rawPassword)
                .type(UserType.ADMIN)
                .build();

        // when & then
        assertThatThrownBy(() -> userInfoService.createUser(duplicateReq))
                .isInstanceOf(UserInfoException.ConflictEx.class)
                .hasMessage("이미 존재하는 사용자ID 입니다.");
    }

    @Test
    @DisplayName("사용자 정보를 수정한다.")
    void updateUser_Success() {
        // given
        UserInfo user = userInfoRepository.findByUsername("admin").get();
        UserInfoDTO.UpdateReq updateReq = UserInfoDTO.UpdateReq.builder()
                .name("New Name")
                .email("new@email.com")
                .build();

        // when
        UserInfo updatedUser = userInfoService.updateUser(user.getId(), updateReq);

        // then
        assertThat(updatedUser.getName()).isEqualTo("New Name");
        assertThat(updatedUser.getEmail()).isEqualTo("new@email.com");
    }

    @Test
    @DisplayName("사용자 비밀번호를 변경한다.")
    void changePassword_Success() {
        // given
        UserInfo user = userInfoRepository.findByUsername("admin").get();
        String newPassword = "new_password";

        // when
        userInfoService.changePassword(user.getId(), rawPassword, newPassword);

        // then
        UserInfo updatedUser = userInfoRepository.findById(user.getId()).get();
        assertThat(updatedUser.getPassword().isMatched(newPassword)).isTrue();
    }

    @Test
    @DisplayName("사용자를 삭제한다.")
    void deleteUser_Success() {
        // given
        UserInfo user = userInfoRepository.findByUsername("admin").get();
        Long userId = user.getId();

        // when
        userInfoService.deleteUser(userId);

        // then
        assertThat(userInfoRepository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("모든 사용자를 조회한다.")
    void getUsers_Success() {
        // when
        List<UserInfo> users = userInfoService.getUsers();

        // then
        assertThat(users).hasSize(1);
    }
} 