package com.ltalk.web.global.websocket;

import com.ltalk.web.global.dto.LoginMemberDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    WsHandshakeInterceptor wsHandshakeInterceptor;
    StompChannelInterceptor stompChannelInterceptor;


    public WebSocketConfig(WsHandshakeInterceptor wsHandshakeInterceptor, StompChannelInterceptor stompChannelInterceptor) {
        this.wsHandshakeInterceptor = wsHandshakeInterceptor;
        this.stompChannelInterceptor = stompChannelInterceptor;
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
        WebSocketMessageBrokerConfigurer.super.configureWebSocketTransport(registry);
    }
}
