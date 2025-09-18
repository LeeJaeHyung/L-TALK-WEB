package com.ltalk.web.chat.dto.response;

public interface ChatRoomSummaryProjection {
    Long getChatRoomId();
    String getChatRoomName();
    String getChatRoomType();
    Integer getParticipantCount();
    String getMembersJson();
    String getLastChatJson();
}
