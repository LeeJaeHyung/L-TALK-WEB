package com.ltalk.web.entity;

import com.ltalk.web.enums.ChatRoomType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoom {
    private Long id;
    private String name;
    private ChatRoomType type;
    private int participantCount;
    private List<ChatRoomMember> memberList;
    private List<Chat> chatList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
