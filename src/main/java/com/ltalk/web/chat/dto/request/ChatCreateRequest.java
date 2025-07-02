package com.ltalk.web.chat.dto.request;

import lombok.Data;

@Data
public class ChatCreateRequest {
    private Long senderId;
    private String message;
}

