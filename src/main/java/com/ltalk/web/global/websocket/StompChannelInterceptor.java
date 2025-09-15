package com.ltalk.web.global.websocket;

import com.ltalk.web.chatroom.service.ChatRoomService;
import com.ltalk.web.global.dto.LoginMemberDto;
import io.lettuce.core.RedisCredentialsProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final AntPathMatcher PATH = new AntPathMatcher();
    private static final String PATTERN = "/topic/chatrooms/{roomId}/chats";
    private final ChatRoomService chatRoomService;

    public StompChannelInterceptor(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // STOMP 헤더 파싱
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) return message; // 하트비트 등은 command 없음

        switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            case SEND -> handleSend(accessor, message);
            case DISCONNECT -> handleDisconnect(accessor);
            default -> {} // 기타 (예: HEARTBEAT)
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        // 핸드셰이크에서 넣어둔 값
        if (attrs == null) {
            throw new IllegalStateException("세션 속성이 없습니다");
        }
        String memberId = String.valueOf(attrs.get("memberId"));
        if (memberId == null) {
            throw new IllegalStateException("인증되지 않은 사용자");
        }

        System.out.println("연결 되었습니다. memberId = " + memberId);

        Principal principal = accessor.getUser();
        if (principal != null) {
            System.out.println("principal: "+principal.getName());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        // 구독 경로 유효성 검사, 권한 확인
        if(accessor.getUser()==null) new IllegalArgumentException("접속가능한 사용자가 아님");
        String destination = accessor.getDestination();
        canSubscribe(accessor.getUser().getName(),destination);
    }

    private void handleSend(StompHeaderAccessor accessor, Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[] data) {
            String body = new String(data, StandardCharsets.UTF_8);
            System.out.println("받은 메시지(body): " + body);
        } else {
            System.out.println("Payload: " + payload);
        }
    }


    private void handleDisconnect(StompHeaderAccessor accessor) {
        // 접속 종료 시 세션 정리
    }

    private void canSubscribe(String memberIdStr, String dest) {
        if (!PATH.match(PATTERN, dest)) throw new IllegalArgumentException("Unsupported destination: " + dest);
        String roomIdStr = PATH.extractUriTemplateVariables(PATTERN, dest).get("roomId");
        System.out.println("구독 요청 MemberID:"+memberIdStr+" roomID:"+roomIdStr);
        if (roomIdStr == null || !roomIdStr.matches("\\d+")) throw new IllegalArgumentException("Invalid roomId: " + roomIdStr);
        Long roomId = Long.parseLong(roomIdStr);
        Long memberId = Long.parseLong(memberIdStr);
        if(!chatRoomService.canSubscribe(memberId, roomId)){
            System.out.println("챗룸 맴버없음");
            throw new IllegalStateException("채팅방에 참여된 맴버가 아닙니다.");
        }
        System.out.println("구독 요청승인!!     MemberID:"+memberIdStr+" roomID:"+roomIdStr);
    }
}