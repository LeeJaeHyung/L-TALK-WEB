package com.ltalk.web.entity;

import com.ltalk.web.enums.FriendStatus;

public class Friend {
    private Member fromMember;
    private Member toMember;
    private FriendStatus status;
    public Friend(Member fromMember, Member toMember, FriendStatus status) {
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.status = status;
    }
}
