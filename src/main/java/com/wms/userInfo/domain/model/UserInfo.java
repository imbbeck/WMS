package com.wms.user.domain.model;

import java.util.regex.Pattern;

import com.wms.applicationInfra.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo extends BaseEntity {

    @Column(name= "username", nullable = false, unique = true, length = 20)
    private String username; // 로그인 ID

    @Column(name= "name", nullable = false)
    private String name;

    @Column(name= "email", nullable = false, unique = true)
    private String email;

    @Embedded
    private Password password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    @Builder
    public UserInfo(String username, String name, String email, Password password, UserType type) {
        validateUsername();
        this.username = username;
        this.name = name;
        this.email = email;
        this.password = password;
        this.type = type;
    }

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    public void validateUsername() {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Username must contain only lowercase letters, numbers, and underscores.");
        }
    }

    public void update(String name, String email){
        this.name = name;
        this.email = email;
    }

    public boolean isAdmin() {
        return this.type == UserType.ADMIN;
    }

    public boolean isWorker() {
        return this.type == UserType.WORKER;
    }

    // Worker 관련 메서드
    public boolean canAccessWorkerDashboard() {
        return isWorker();
    }

    public boolean canAccessAdminDashboard() {
        return isAdmin();
    }
}
