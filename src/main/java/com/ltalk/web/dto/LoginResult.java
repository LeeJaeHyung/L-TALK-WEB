package com.ltalk.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResult {
    String token;
    LoginMemberDto loginMember;
}
