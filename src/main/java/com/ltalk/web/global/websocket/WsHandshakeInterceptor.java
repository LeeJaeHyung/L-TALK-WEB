package com.ltalk.web.global.websocket;

import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.global.service.RedisLoginTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WsHandshakeInterceptor implements HandshakeInterceptor {// 핸드쉐이크시 인터셉트

    private final RedisLoginTokenService redisLoginTokenService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest req,               // ① 추상 HTTP 요청
            ServerHttpResponse res,              // ② 추상 HTTP 응답
            WebSocketHandler wsHandler,          // ③ 이후 사용할 WS 핸들러
            Map<String, Object> attributes) {    // ④ WS 세션에 보관되는 키-값 저장소
        System.out.println("beforeHandshake");
        if (req instanceof ServletServerHttpRequest sreq) {
            HttpServletRequest http = sreq.getServletRequest();// ⑤ 서블릿 요청으로 캐스팅
            Cookie[] cookies = http.getCookies();
            if (cookies != null) {
                for(Cookie cookie : cookies) {
                    if(cookie.getName().equals("access_token")) {
                        String accessToken = cookie.getValue();
                        LoginMemberDto loginMemberDto = redisLoginTokenService.get(accessToken);
                        if(loginMemberDto != null) {
                            attributes.put("memberId", String.valueOf(loginMemberDto.getId()));
                            attributes.put("token", accessToken);
                            return true;
                        }else{
                            if (res instanceof org.springframework.http.server.ServletServerHttpResponse sres) {
                                Cookie kill = new Cookie("access_token", "");
                                kill.setMaxAge(0);
                                kill.setPath("/");
                                sres.getServletResponse().addCookie(kill);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return false; // ⑧ false를 반환하면 업그레이드(연결) 거절
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        System.out.println("afterHandshake");
    }
}
