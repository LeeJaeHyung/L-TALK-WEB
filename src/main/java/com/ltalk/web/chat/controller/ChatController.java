package com.ltalk.web.chat.controller;

import com.ltalk.web.chat.dto.ChatMessage;
import com.ltalk.web.chat.dto.request.ChatCreateRequest;
import com.ltalk.web.chat.service.ChatService;
import com.ltalk.web.global.dto.LoginMemberDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/chatrooms")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate template;


    public ChatController(ChatService chatService, SimpMessagingTemplate template) {
        this.chatService = chatService;
        this.template = template;
    }
    @PostMapping("/{chatRoomId}/chats")
    public ResponseEntity<Void> createChat(HttpServletRequest request, @PathVariable Long chatRoomId,
                                           @RequestBody ChatCreateRequest chatCreateRequest) {
        Long senderId = ((LoginMemberDto)request.getAttribute("member")).getId();
        chatService.createChat(chatRoomId, chatCreateRequest, senderId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @MessageMapping("/chat.send") // 클라: /app/chat.send
    public void send(ChatMessage msg, Principal principal) {
        // ChannelInterceptor에서 setUser 한 값
        msg.setSender(principal.getName());
        msg.setTimestamp(System.currentTimeMillis());

        // 방 브로드캐스트
        template.convertAndSend("/topic/room." + msg.getRoomId(), msg);

        // (선택) 보낸 이에게만 ACK
        // template.convertAndSendToUser(principal.getName(), "/queue/ack", Map.of("ok", true));
    }
}
