package com.ltalk.web.entity;

import com.ltalk.web.enums.UserRole;
import com.ltalk.web.request.SignUpRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor  // JPA는 기본 생성자가 꼭 필요해!
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL의 AUTO_INCREMENT에 대응
    private Long id;

    @Column(nullable = false, unique = true) // 중복 방지
    private String userName;

    @Column(nullable = false)
    private String nickName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String salt;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING) // Enum 저장 시 이름(문자열)으로 저장
    private UserRole userRole;

    public Member(String userName, String nickName, String password, String email, String phoneNumber, UserRole userRole) {
        this.userName = userName;
        this.nickName = nickName;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userRole = userRole;
    }

    public Member(SignUpRequest request, String salt) {
        this.userName = request.getUserName();
        this.nickName = request.getNickName();
        this.password = request.getPassword();
        this.salt = salt;
        this.email = request.getEmail();
        this.phoneNumber = request.getPhoneNumber();
        this.userRole = UserRole.USER;
    }
}
