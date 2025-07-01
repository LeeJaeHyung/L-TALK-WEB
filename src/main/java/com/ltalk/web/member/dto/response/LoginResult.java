package com.ltalk.web.member.dto.response;

import com.ltalk.web.global.dto.LoginMemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResult {
    String token;
    LoginMemberDto loginMember;
}
