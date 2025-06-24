package com.ltalk.web.dto.response;

import com.ltalk.web.entity.Friend;
import lombok.Getter;

@Getter
public class RequestFriendResponse {
    private Friend friend;
    public RequestFriendResponse(Friend friend) {
        this.friend = friend;
    }
}
