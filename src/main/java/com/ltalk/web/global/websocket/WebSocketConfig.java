package com.ltalk.web.global.websocket;

import com.ltalk.web.global.dto.LoginMemberDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    WsHandshakeInterceptor wsHandshakeInterceptor;
    StompChannelInterceptor stompChannelInterceptor;
    private final WsSessionRegistry wsSessionRegistry;


    public WebSocketConfig(WsHandshakeInterceptor wsHandshakeInterceptor, StompChannelInterceptor stompChannelInterceptor, WsSessionRegistry wsSessionRegistry) {
        this.wsHandshakeInterceptor = wsHandshakeInterceptor;
        this.stompChannelInterceptor = stompChannelInterceptor;
        this.wsSessionRegistry = wsSessionRegistry;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").addInterceptors(wsHandshakeInterceptor).setHandshakeHandler(new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                Object mid = attributes.get("memberId");   // WsHandshakeInterceptor가 넣음
                if (mid == null) return null;              // 비인증이면 null (네 정책대로)
                String name = mid.toString();
                return () -> name;                         // 세션 Principal 확정
            }
        }).setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
       registry.enableSimpleBroker("/topic", "/queue");
       registry.setApplicationDestinationPrefixes("/app");
       registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                var attrs = session.getAttributes();
                Object userId = attrs.get("memberId");
                Object token  = attrs.get("token");
                wsSessionRegistry.register(userId, token, session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                var attrs = session.getAttributes();
                Object userId = attrs.get("memberId");
                Object token  = attrs.get("token");
                wsSessionRegistry.unregister(userId, token, session);
                super.afterConnectionClosed(session, status);
            }
        });
    }
}
