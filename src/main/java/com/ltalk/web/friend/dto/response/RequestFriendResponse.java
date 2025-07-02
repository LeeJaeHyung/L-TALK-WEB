package com.ltalk.web.friend.dto.response;


import com.ltalk.web.friend.domain.Friend;
import lombok.Getter;

@Getter
public class RequestFriendResponse {
    private Friend friend;
    public RequestFriendResponse(Friend friend) {
        this.friend = friend;
    }
}
