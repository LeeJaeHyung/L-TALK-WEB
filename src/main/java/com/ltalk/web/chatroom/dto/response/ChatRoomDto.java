package com.ltalk.web.chatroom.dto.response;

import com.ltalk.web.chat.dto.response.ChatDto;

import java.util.List;

public record ChatRoomDto(
        Long id,
        String name,
        String type,
        int participantCount,
        List<ChatRoomMemberDto> members,
        List<ChatDto> chats
) {}
