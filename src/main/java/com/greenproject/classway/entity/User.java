package com.greenproject.classway.entity;

import com.greenproject.classway.global.UserProvider;
import com.greenproject.classway.global.UserRole;
import com.greenproject.classway.global.UserState;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserProvider provider = UserProvider.LOCAL;

    @Column(name = "provider_user_id", length = 100)
    private String providerUserId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.STUDENT;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserState status = UserState.ACTIVE;

    @Column(name = "point_balance", nullable = false)
    private Integer pointBalance = 0;

    @Column(name = "login_fail_count", nullable = false)
    private Integer loginFailCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "class_alarm", nullable = false)
    private Boolean classAlarm = true;

    @Column(name = "marketing_agree", nullable = false)
    private Boolean marketingAgree = false;

    @Column(name = "terms_agreed_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime termsAgreedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
}
