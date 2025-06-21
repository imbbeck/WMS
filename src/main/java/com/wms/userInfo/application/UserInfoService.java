package com.wms.userInfo.application;

import java.util.List;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.userInfo.domain.exception.UserInfoException;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.userInfo.dto.UserInfoDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;

    @Transactional
    public UserInfo createUser(UserInfoDTO.CreateReq request) {
        UserInfo userInfo = saveUserWithValidation(request.getUserId(), request.getEmail(),request.toEntity());

        return userInfo;
    }

    public UserInfo getUserById(Long id) {
        return userInfoRepository.findById(id)
                .orElseThrow(() -> UserInfoException.notFound(id));
    }

    public UserInfo getUserByUserName(String userId) {
        return userInfoRepository.findByUsername(userId)
                .orElseThrow(() -> UserInfoException.notFound(userId));
    }

    public List<UserInfo> getUsers() {
        return userInfoRepository.findAll();
    }

    public List<UserInfo> getUsersByType(UserType type) {
        return userInfoRepository.findAllByType(type);
    }

    @Transactional
    public UserInfo join(UserInfoDTO.JoinReq request) {
        UserInfo userInfo = saveUserWithValidation(request.getUserId(), request.getEmail(),request.toEntity());

         return userInfo;
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
    public void withdraw(UserInfo currentUser) {
        userInfoRepository.delete(currentUser);
    }

    private UserInfo saveUserWithValidation(String userId, String email, UserInfo user) {
        if (userInfoRepository.existsByUsername(userId)) {
            throw UserInfoException.duplicate(FieldEnum.USERID);
        }
        if (userInfoRepository.existsByEmail(email)) {
            throw UserInfoException.duplicate(FieldEnum.EMAIL);
        }
        return userInfoRepository.save(user);
    }
}