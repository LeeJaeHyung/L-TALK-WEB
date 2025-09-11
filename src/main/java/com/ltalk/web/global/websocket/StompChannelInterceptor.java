package com.ltalk.web.global.websocket;

import com.ltalk.web.global.dto.LoginMemberDto;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class StompChannelInterceptor implements ChannelInterceptor {

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
        System.out.println(accessor.getDestination());
        String[] destinations = Objects.requireNonNull(accessor.getDestination()).split("/");
        System.out.println("destinations.length = " + destinations.length);
        for (String destination : destinations) {
            System.out.println(destination);
        }
        System.out.println("destinations  끝");

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
}