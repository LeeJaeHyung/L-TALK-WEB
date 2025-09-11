package com.ltalk.web.chat.dto.response;

import java.time.LocalDateTime;

public record ChatDto(
        Long id,
        Long senderId,
        Long chatRoomId,
        String message,
        LocalDateTime createdAt
) {}
