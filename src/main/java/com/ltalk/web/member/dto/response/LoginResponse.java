package com.ltalk.web.member.dto.response;

import com.ltalk.web.global.dto.LoginMemberDto;
import lombok.Getter;

@Getter
public class LoginResponse {

    private String message;
    private String token;
    private LoginMemberDto memberDto;

    public LoginResponse(String message, String token, LoginMemberDto memberDto) {
        this.message = message;
        this.token = token;
        this.memberDto = memberDto;
    }
}
