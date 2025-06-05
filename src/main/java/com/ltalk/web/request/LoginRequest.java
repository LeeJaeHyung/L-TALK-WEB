package com.ltalk.web.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {
    String userName;
    String password;
}
