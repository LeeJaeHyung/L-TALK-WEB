package com.ltalk.web.dto.request;

import com.ltalk.web.enums.ChatRoomType;
import lombok.Data;

import java.util.List;

@Data
public class ChatRoomCreateRequest {
    private String name;
    private ChatRoomType type;
    private List<Long> memberList;
}
