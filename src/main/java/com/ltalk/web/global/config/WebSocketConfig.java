package com.ltalk.web.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WsHandshakeAuthInterceptor handshakeAuthInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(WsHandshakeAuthInterceptor handshakeAuthInterceptor,
                           StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.handshakeAuthInterceptor = handshakeAuthInterceptor;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")               // 브라우저 연결 진입점
                .addInterceptors(handshakeAuthInterceptor) // 쿠키/토큰 검증
                .setAllowedOriginPatterns("*")
                .withSockJS();                         // 구형 브라우저 폴백 (선택)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 개발/단일 서버
        registry.enableSimpleBroker("/topic", "/queue"); // 구독 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 발신 prefix
        registry.setUserDestinationPrefix("/user"); // 1:1 큐 prefix

        // 운영/다중 서버 시(외부 브로커 사용):
        // registry.enableStompBrokerRelay("/topic", "/queue")
        //         .setRelayHost("localhost").setRelayPort(61613)
        //         .setClientLogin("guest").setClientPasscode("guest")
        //         .setSystemLogin("guest").setSystemPasscode("guest");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor); // STOMP 프레임 인증/권한
    }
}
