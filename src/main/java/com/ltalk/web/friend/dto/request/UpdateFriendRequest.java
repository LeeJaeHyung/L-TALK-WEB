package com.ltalk.web.friend.dto.request;

import com.ltalk.web.friend.domain.FriendStatus;
import lombok.Data;

@Data
public class UpdateFriendRequest {
    FriendStatus status;
}
