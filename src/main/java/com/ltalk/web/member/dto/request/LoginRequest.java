package com.ltalk.web.member.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {
    String username;
    String password;
}
