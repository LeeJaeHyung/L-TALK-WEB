package com.ltalk.web.member.domain;

import com.ltalk.web.member.dto.request.SignUpRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@ToString
@NoArgsConstructor
@Where(clause = "deleted_at IS NULL")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false, unique = true)
    private String nickName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String salt;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    private LocalDateTime deletedAt; // ✅ 삭제 시각

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    public Member(String userName, String nickName, String password, String email, String phoneNumber, UserRole userRole, String language) {
        this.userName = userName;
        this.nickName = nickName;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userRole = userRole;
        this.salt = "";
        this.language = language;
    }

    public Member(SignUpRequest request, String salt) {
        this.userName = request.getUserName();
        this.nickName = request.getNickName();
        this.password = request.getPassword();
        this.salt = salt;
        this.email = request.getEmail();
        this.phoneNumber = request.getPhoneNumber();
        this.userRole = UserRole.USER;
        this.language = request.getLanguage();
    }

    public void update(String nickName, String password, String email, String phoneNumber, String language) {
        if (nickName != null) this.nickName = nickName;
        if (password != null) this.password = password;
        if (email != null) this.email = email;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (language != null) this.language = language;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now(); // ✅ 삭제 시각 기록
    }
}
