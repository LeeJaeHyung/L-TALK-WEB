package com.ltalk.web.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {

    private String message;
    private String token;

    public LoginResponse(String message, String token) {
        this.message = message;
        this.token = token;
    }
}
