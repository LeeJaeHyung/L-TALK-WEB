package com.ltalk.web.friend.dto.response;


import com.ltalk.web.friend.domain.Friend;
import lombok.Getter;

import java.util.List;

@Getter
public class FriendListResponse {
    private List<Friend> friends;

    public FriendListResponse(List<Friend> friends) {
        this.friends = friends;
    }
}
