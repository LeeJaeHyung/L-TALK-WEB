package com.ltalk.web.global.websocket;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {// 핸드쉐이크시 인터셉트
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest req,               // ① 추상 HTTP 요청
            ServerHttpResponse res,              // ② 추상 HTTP 응답
            WebSocketHandler wsHandler,          // ③ 이후 사용할 WS 핸들러
            Map<String, Object> attributes) {    // ④ WS 세션에 보관되는 키-값 저장소
        if (req instanceof ServletServerHttpRequest sreq) {
            HttpServletRequest http = sreq.getServletRequest(); // ⑤ 서블릿 요청으로 캐스팅
            String tag = http.getParameter("tag");              // ⑥ 쿼리 파라미터 예시
            attributes.put("tag", tag != null ? tag : "guest"); // ⑦ STOMP 단계에서 사용될 값 저장
        }
        return true; // ⑧ false를 반환하면 업그레이드(연결) 거절
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
