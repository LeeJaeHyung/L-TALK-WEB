package com.ltalk.web.global.service;

import com.google.gson.Gson;
import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.global.websocket.WsSessionRegistry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class RedisLoginTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Gson gson;
    private final WsSessionRegistry wsSessionRegistry;

    public RedisLoginTokenService(RedisTemplate<String, String> redisTemplate, WsSessionRegistry wsSessionRegistry) {
        this.redisTemplate = redisTemplate;
        this.gson = new Gson();
        this.wsSessionRegistry = wsSessionRegistry;// 기본 Gson 인스턴스
    }

    public void save(String token, LoginMemberDto member) {
        String json = gson.toJson(member);
        Long memberId = member.getId();
        redisTemplate.opsForValue().set("access_token:" + token, json, Duration.ofMinutes(30));
        String beforeToken = redisTemplate.opsForValue().get("member_id:" + memberId);
        if (beforeToken != null) {
            redisTemplate.delete("access_token:"+beforeToken);
            try {
                System.out.println("기존 소켓연결 존재 그래서 소켓 연결끊음");
                wsSessionRegistry.disconnectByToken(beforeToken, CloseStatus.POLICY_VIOLATION);
                wsSessionRegistry.disconnectByUser(memberId, CloseStatus.POLICY_VIOLATION);
            } catch (Exception ignore) { /* 로그만 남겨도 됨 */}

        }
        redisTemplate.opsForValue().set("member_id:" + memberId, token, Duration.ofMinutes(30));

    }

    public LoginMemberDto get(String token) {
        String json = redisTemplate.opsForValue().get("access_token:" + token);
        if (json == null) return null;
        return gson.fromJson(json, LoginMemberDto.class);
    }

    public void remove(String token, Long memberId) {
        redisTemplate.delete("access_token:"+token);
        redisTemplate.delete("member_id:" + memberId);
    }
}
