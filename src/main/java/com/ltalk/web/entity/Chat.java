package com.ltalk.web.entity;

import java.time.LocalDateTime;

public class Chat {
    private long id;
    private ChatRoom chatRoom;
    private ChatRoomMember sender;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
