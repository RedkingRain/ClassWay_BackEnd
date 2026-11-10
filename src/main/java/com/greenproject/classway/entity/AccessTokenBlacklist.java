package com.greenproject.classway.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_token_blacklist")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AccessTokenBlacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 700)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expirationDatetime;
}
