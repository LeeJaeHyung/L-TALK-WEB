package com.ltalk.web.member.dto.request;

import lombok.*;


@AllArgsConstructor
@ToString
@Getter
@Setter
public class MemberPatchRequest {
    String nickname;
    String password;
    String email;
    String phoneNumber;
    String language;
}
