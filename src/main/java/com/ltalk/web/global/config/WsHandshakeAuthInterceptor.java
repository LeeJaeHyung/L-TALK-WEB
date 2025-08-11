package com.ltalk.web.global.config;

import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.global.service.RedisLoginTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WsHandshakeAuthInterceptor implements HandshakeInterceptor {

    private final RedisLoginTokenService redis;

    public WsHandshakeAuthInterceptor(RedisLoginTokenService redis) {
        this.redis = redis;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var sreq = (ServletServerHttpRequest) req;
        var sres = (ServletServerHttpResponse) res;
        HttpServletRequest http = sreq.getServletRequest();
        HttpServletResponse httpRes = sres.getServletResponse();

        // 쿠키에서 access_token 조회
        String token = null;
        if (http.getCookies() != null) {
            for (Cookie c : http.getCookies()) {
                if ("access_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) return false;

        LoginMemberDto member = redis.get(token); // 유효하지 않으면 null
        if (member == null) {
            Cookie kill = new Cookie("access_token", "");
            kill.setMaxAge(0); kill.setPath("/");
            httpRes.addCookie(kill);
            return false;
        }

        attributes.put("memberId", member.getId());
        attributes.put("memberName", member.getUserName());
        return true;
    }

    @Override public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                         WebSocketHandler wsHandler, Exception ex) { }
}
