package com.ltalk.web.chatroom.dto.request;

import com.ltalk.web.chatroom.domain.ChatRoomType;
import com.ltalk.web.member.domain.Member;
import lombok.Data;

import java.util.List;

@Data
public class ChatRoomCreateRequest {
    private String name;
    private ChatRoomType type;
    private List<Long> memberIds;
}
