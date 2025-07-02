package com.ltalk.web.chat.controller;

import com.ltalk.web.chat.dto.request.ChatCreateRequest;
import com.ltalk.web.chat.service.ChatService;
import com.ltalk.web.global.dto.LoginMemberDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatrooms")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{chatRoomId}/chats")
    public ResponseEntity<Void> createChat(HttpServletRequest request, @PathVariable Long chatRoomId,
                                           @RequestBody ChatCreateRequest chatCreateRequest) {
        Long senderId = ((LoginMemberDto)request.getAttribute("member")).getId();
        chatService.createChat(chatRoomId, chatCreateRequest, senderId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
