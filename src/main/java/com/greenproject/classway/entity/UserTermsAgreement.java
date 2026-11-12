package com.greenproject.classway.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_terms_agreements")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserTermsAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id", nullable = false)
    private Terms terms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_agreed", nullable = false)
    private Boolean isAgreed;

    @Column(name = "agreed_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime agreedAt;

    @Column(name = "ip_address", nullable = false, columnDefinition = "VARBINARY(16)")
    private byte[] ipAddress;
}
