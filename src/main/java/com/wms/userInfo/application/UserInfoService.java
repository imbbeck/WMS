package com.wms.userInfo.application;

import java.util.List;

import com.wms.applicationInfra.domain.FieldEnum;
import com.wms.userInfo.domain.event.UserInfoCreatedEvent;
import com.wms.userInfo.domain.event.UserInfoDeletedEvent;
import com.wms.userInfo.domain.event.UserInfoUpdatedEvent;
import com.wms.userInfo.domain.exception.UserInfoException;
import com.wms.userInfo.domain.model.UserInfo;
import com.wms.userInfo.domain.model.UserType;
import com.wms.userInfo.domain.repository.UserInfoRepository;
import com.wms.userInfo.dto.UserInfoDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserInfo createUser(UserInfoDTO.CreateReq request) {
        checkDuplication(request.getUserId(), request.getEmail());

        UserInfo userInfo = request.toEntity();

		UserInfo saved = userInfoRepository.save(userInfo);

        eventPublisher.publishEvent(new UserInfoCreatedEvent(userInfo.getId(), userInfo.getName()));

        return saved;
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
        checkDuplication(request.getUserId(), request.getEmail());

        UserInfo userInfo = request.toEntity();

		UserInfo saved = userInfoRepository.save(userInfo);

        eventPublisher.publishEvent(new UserInfoCreatedEvent(userInfo.getId(), userInfo.getName()));

        return saved;
    }

    @Transactional
    public UserInfo updateUser(Long id, UserInfoDTO.UpdateReq request) {
        UserInfo userInfo = getUserById(id);
        userInfo.update(request.getName(), request.getEmail());

        eventPublisher.publishEvent(new UserInfoUpdatedEvent(userInfo.getId(), userInfo.getName()));

        return userInfo;
    }

    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        UserInfo user = getUserById(id);
        user.changePassword(newPassword, oldPassword);
    }

    @Transactional
    public void deleteUser(Long id) {
        UserInfo userinfo = getUserById(id);

        // 삭제 이벤트 발행
        eventPublisher.publishEvent(new UserInfoDeletedEvent(userinfo.getId()));

        userInfoRepository.delete(userinfo);
    }

    @Transactional
    public void withdraw(UserInfo currentUser) {
        userInfoRepository.delete(currentUser);
    }

    private void checkDuplication(String userId, String email) {
        if (userInfoRepository.existsByUsername(userId)) {
            throw UserInfoException.duplicate(FieldEnum.USERID);
        }
        if (userInfoRepository.existsByEmail(email)) {
            throw UserInfoException.duplicate(FieldEnum.EMAIL);
        }
    }
}