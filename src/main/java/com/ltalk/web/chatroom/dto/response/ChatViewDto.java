package com.ltalk.web.chatroom.dto.response;

import java.time.LocalDateTime;

public record ChatViewDto(
        Long id,
        Long chatRoomId,
        String senderNickname,
        String message,
        LocalDateTime createdAt
) {}