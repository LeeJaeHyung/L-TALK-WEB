package com.ltalk.web.dto.response;

import com.ltalk.web.dto.LoginMemberDto;
import com.ltalk.web.entity.Member;
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
