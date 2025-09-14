package com.ltalk.web.chat.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ltalk.web.global.config.LenientLocalDateTimeDeserializer;

import java.time.LocalDateTime;

public record ChatDto(
        Long id,
        Long senderId,
        Long chatRoomId,
        String message,
        @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class)
        LocalDateTime createdAt
) {}
