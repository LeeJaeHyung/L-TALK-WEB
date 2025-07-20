package com.ltalk.web.chatroom.dto.response;

public record ChatRoomMemberDto(
        Long id,
        Long memberId,
        String username
) {}
