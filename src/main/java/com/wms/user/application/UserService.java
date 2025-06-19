//package com.wms.user.application;
//
//import com.wms.user.domain.model.Password;
//import com.wms.user.domain.model.UserInfo;
//import com.wms.user.domain.repository.UserRepository;
//import com.wms.user.dto.UserRequest;
//import com.wms.user.mapper.UserMapper;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class UserService {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final UserMapper userMapper;
//
//    @Transactional
//    public UserInfo createUser(UserRequest request) {
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
//        }
//        UserInfo userInfo = UserInfo.builder()
//                .name(request.getName())
//                .email(request.getEmail())
//                .password(Password.builder().value(request.getPassword()).build())
//                .type(request.getType())
//                .build();
//
//        return userRepository.save(userInfo);
//    }
//
//    public UserInfo getUser(Long id) {
//        return userRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
//    }
//
//    public UserInfo getUserByEmail(String email) {
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
//    }
//}