package com.wms.userInfo.application;

import java.util.List;

import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.userInfo.dto.UserInfoDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;

    @Transactional
    public UserInfo createUser(UserInfoDTO.CreateReq request) {
        if (userInfoRepository.existsByUsername(request.getUserId())) {
            throw new IllegalArgumentException("이미 존재하는 userId입니다.");
        }
        if (userInfoRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        UserInfo user = request.toEntity();
        return userInfoRepository.save(user);
    }

    public UserInfo getUserByUserName(String userId) {
        return userInfoRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 user ID의 사용자를 찾을 수 없습니다."));
    }

    public List<UserInfo> getUsers() {
        return userInfoRepository.findAll();
    }

    public List<UserInfo> getUsersByType(UserType type) {
        return userInfoRepository.findAllByType(type);
    }

    @Transactional
    public UserInfo join(UserInfoDTO.JoinReq request) {
        if (userInfoRepository.existsByUsername(request.getUserId())) {
            throw new IllegalArgumentException("이미 존재하는 userId입니다.");
        }
        if (userInfoRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        UserInfo user = request.toEntity();
        return userInfoRepository.save(user);
    }

    @Transactional
    public UserInfo updateUser(Long id, UserInfoDTO.UpdateReq request) {
        UserInfo user = getUserById(id);
        user.update(request.getName(), request.getEmail());
        return user;
    }

    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        UserInfo user = getUserById(id);
        user.changePassword(newPassword, oldPassword);
    }

    @Transactional
    public void deleteUser(Long id) {
        UserInfo user = getUserById(id);
        userInfoRepository.delete(user);
    }

    @Transactional
    public void withdraw(Long userIdx, UserInfo currentUser) {
        if (currentUser.getType() != UserType.WORKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only WORKER users can withdraw");
        }

        userInfoRepository.delete(currentUser);
    }

    private UserInfo getUserById(Long id) {
        return userInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 idx의 사용자를 찾을 수 없습니다."));
    }
}