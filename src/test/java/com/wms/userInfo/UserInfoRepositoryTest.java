package com.wms.userInfo;

import com.wms.applicationInfra.config.QuerydslConfig;
import com.wms.userInfo.domain.model.Password;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class UserInfoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private UserInfoRepository userInfoRepository;

	@BeforeEach
    void setUp() {
	    UserInfo admin = UserInfo.builder()
			    .username("admin_user")
			    .name("admin")
			    .email("admin@test.com")
			    .password(new Password("password"))
			    .type(UserType.ADMIN)
			    .build();
		UserInfo worker = UserInfo.builder()
				.username("worker_user")
				.name("worker")
				.email("worker@test.com")
				.password(new Password("password"))
				.type(UserType.WORKER)
				.build();
        entityManager.persist(admin);
        entityManager.persist(worker);
        entityManager.flush();
    }

    @Test
    @DisplayName("username으로 사용자를 조회한다.")
    void findByUsername_Success() {
        Optional<UserInfo> foundUser = userInfoRepository.findByUsername("admin_user");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("admin_user");
    }

    @Test
    @DisplayName("존재하지 않는 username으로 조회 시 empty Optional을 반환한다.")
    void findByUsername_NotFound_ReturnsEmpty() {
        Optional<UserInfo> foundUser = userInfoRepository.findByUsername("non_existent_user");
        assertThat(foundUser).isNotPresent();
    }

    @Test
    @DisplayName("특정 타입의 모든 사용자를 조회한다.")
    void findAllByType_Success() {
        List<UserInfo> admins = userInfoRepository.findAllByType(UserType.ADMIN);
        List<UserInfo> workers = userInfoRepository.findAllByType(UserType.WORKER);
        assertThat(admins).hasSize(1).extracting("type").containsOnly(UserType.ADMIN);
        assertThat(workers).hasSize(1).extracting("type").containsOnly(UserType.WORKER);
    }

    @Test
    @DisplayName("username 존재 여부를 확인한다.")
    void existsByUsername_Success() {
        boolean exists = userInfoRepository.existsByUsername("admin_user");
        boolean notExists = userInfoRepository.existsByUsername("non_existent_user");
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("email 존재 여부를 확인한다.")
    void existsByEmail_Success() {
        boolean exists = userInfoRepository.existsByEmail("worker@test.com");
        boolean notExists = userInfoRepository.existsByEmail("non_existent@email.com");
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
} 