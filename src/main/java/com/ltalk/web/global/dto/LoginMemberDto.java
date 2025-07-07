package com.ltalk.web.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginMemberDto {
    private Long id;
    private String userName;
    private String nickName;
    private String email;
    private String password;
    private String role;
}

