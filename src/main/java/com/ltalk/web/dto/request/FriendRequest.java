package com.ltalk.web.dto.request;

import com.ltalk.web.entity.Member;
import lombok.Getter;

@Getter
public class FriendRequest {
    private Long toMemberId;
}
