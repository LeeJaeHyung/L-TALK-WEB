package com.ltalk.web.chat.controller;

import com.ltalk.web.chat.dto.ChatMessage;
import com.ltalk.web.chat.dto.request.ChatCreateRequest;
import com.ltalk.web.chat.dto.response.ChatDto;
import com.ltalk.web.chat.service.ChatService;
import com.ltalk.web.global.dto.LoginMemberDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

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

    // 브로드캐스트: /app/echo 로 들어오면 /topic/echo 로 뿌림
    @MessageMapping("/echo")
    public void echo(String payload, @Header(name = "simpUser", required = false) Object principal) {
        // payload는 JSON/string 무엇이든; 필요 시 DTO로 바인딩
        template.convertAndSend("/topic/echo", "[ECHO] " + payload);
        // 특정 사용자 개인 큐로도 알림
        if (principal != null) {
            String name = principal.toString(); // WsHandshakeInterceptor에서 setUser(() -> memberId)
            template.convertAndSendToUser(name, "/queue/notify", "개인 알림: " + payload);
        }
    }



    // 클라이언트: SEND to /app/chatrooms/{roomId}/chats
    @MessageMapping("/chatrooms/{roomId}/chats")
    public void publishToRoom(@DestinationVariable Long roomId,
                              ChatCreateRequest req,
                              Principal principal) {
        System.out.println(principal.getName());

        Long senderId = resolveSenderId(principal);     // ← 여기서 인증객체에서 추출

        ChatDto dto = new ChatDto(
                null,                                   // id: DB 저장 시 채우기
                senderId,
                roomId,
                req.getMessage(),                          // chatMessage = message 와 동일 사용
                LocalDateTime.now()
        );
        System.out.println("dto : "+dto.toString());

        chatService.createChat(roomId, req, senderId);
        System.out.println("insert 완료");
        template.convertAndSend("/topic/chatrooms/" + roomId + "/chats", dto);
    }



    private Long resolveSenderId(Principal principal) {
        if (principal == null) return 0L;
        // accessor.setUser(() -> memberId) 로 넣었으므로 name == memberId 문자열
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            // memberId가 숫자 문자열이 아니라면 여기서 매핑 규칙에 맞게 처리
            return 0L;
        }
    }
}
