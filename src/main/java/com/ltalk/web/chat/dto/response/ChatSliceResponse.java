package com.ltalk.web.chat.dto.response;

import com.ltalk.web.chatroom.dto.response.ChatViewDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ChatSliceResponse {
    private List<ChatViewDto> content;
    private boolean hasNext;
    private LocalDateTime nextCursorAt;
    private Long nextCursorId;
}