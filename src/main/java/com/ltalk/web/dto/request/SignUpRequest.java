package com.ltalk.web.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SignUpRequest {
    private String userName;
    private String password;
    private String nickName;
    private String email;
    private String phoneNumber;
}
