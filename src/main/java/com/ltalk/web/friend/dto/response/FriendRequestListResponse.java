package com.ltalk.web.friend.dto.response;


import com.ltalk.web.friend.domain.Friend;
import lombok.Getter;

import java.util.List;

@Getter
public class FriendRequestListResponse {
    private List<Friend> myRequestList;
    private List<Friend> otherRequestList;

    public FriendRequestListResponse(List<Friend> myRequestList, List<Friend> otherRequestList) {
        this.myRequestList = myRequestList;
        this.otherRequestList = otherRequestList;
    }
}
