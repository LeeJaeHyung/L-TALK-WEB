package com.ltalk.web.entity;

import java.time.LocalDateTime;

public class ChatRoomMember {
    private long id;
    private ChatRoom chatRoom;
    private Member member;
    private long readChatId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
