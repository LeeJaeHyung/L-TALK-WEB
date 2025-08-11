package com.ltalk.web.global.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var acc = StompHeaderAccessor.wrap(message);
        var cmd = acc.getCommand();
        if (cmd == null) return message;

        if (StompCommand.CONNECT.equals(cmd)) {
            Map<String, Object> attrs = acc.getSessionAttributes();
            if (attrs == null || !attrs.containsKey("memberId")) {
                throw new IllegalStateException("Unauthenticated WebSocket CONNECT");
            }
            String principalName = String.valueOf(attrs.get("memberId"));
            Principal p = () -> principalName; // 시큐리티 없이도 OK
            acc.setUser(p);
        }

        if (StompCommand.SUBSCRIBE.equals(cmd)) {
            // 목적지 권한 체크(예시)
            String dest = acc.getDestination(); // ex) /topic/room.123
            Principal p = acc.getUser();
            if (dest != null && p != null && dest.startsWith("/topic/room.")) {
                String roomId = dest.substring("/topic/room.".length());
                // TODO: roomId, p.getName()로 방 참여 권한 검사 (DB/캐시)
                // if (!allowed) throw new AccessDeniedException("no permission");
            }
        }

        if (StompCommand.SEND.equals(cmd)) {
            // 보낸이 위조 방지(본문 sender vs principal 비교 등)
        }
        return message;
    }
}
