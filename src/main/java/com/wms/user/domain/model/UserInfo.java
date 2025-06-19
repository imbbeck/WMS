//package com.wms.user.domain.model;
//
//import com.wms.common.domain.BaseEntity;
//import jakarta.persistence.Column;
//import jakarta.persistence.Embedded;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.Table;
//import lombok.AccessLevel;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "user_info")
//@Getter
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//public class UserInfo extends BaseEntity {
//
//    @Column(nullable = false)
//    private String name;
//
//    @Column(nullable = false, unique = true)
//    private String email;
//
//    @Embedded
//    private Password password;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private UserType type;
//
//    @Builder
//    public UserInfo(String name, String email, Password password, UserType type) {
//        this.name = name;
//        this.email = email;
//        this.password = password;
//        this.type = type;
//    }
//
//    public boolean isAdmin() {
//        return this.type == UserType.ADMIN;
//    }
//
//    public boolean isWorker() {
//        return this.type == UserType.WORKER;
//    }
//
//    // Worker 관련 메서드
//    public boolean canAccessWorkerDashboard() {
//        return isWorker();
//    }
//
//    public boolean canAccessAdminDashboard() {
//        return isAdmin();
//    }
//}