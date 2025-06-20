package com.wms.userInfo.domain.repository;

import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    Optional<UserInfo> findByUsername(String username);

    List<UserInfo> findAllByType(UserType type);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}