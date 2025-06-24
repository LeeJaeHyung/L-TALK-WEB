package com.ltalk.web.dto.response;

import com.ltalk.web.entity.Friend;
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
