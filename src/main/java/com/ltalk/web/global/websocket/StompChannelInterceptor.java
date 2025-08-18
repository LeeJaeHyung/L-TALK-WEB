package com.ltalk.web.global.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        String memberId = (String) attrs.get("memberId"); // 핸드셰이크에서 넣어둔 값

        if (memberId == null) {
            throw new IllegalStateException("인증되지 않은 사용자");
        }

        // Principal 설정 → 이후 @MessageMapping에서 사용 가능
        accessor.setUser(() -> memberId);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        // 구독 경로 유효성 검사, 권한 확인
    }

    private void handleSend(StompHeaderAccessor accessor, Message<?> message) {
        // 발신 메시지 검증, 위조 방지, 레이트리밋 등
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        // 접속 종료 시 세션 정리
    }
}