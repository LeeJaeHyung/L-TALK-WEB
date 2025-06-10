package com.ltalk.web.service;

import com.google.gson.Gson;
import com.ltalk.web.dto.LoginMemberDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLoginTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Gson gson;

    public RedisLoginTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.gson = new Gson(); // 기본 Gson 인스턴스
    }

    public void save(String token, LoginMemberDto member) {
        String json = gson.toJson(member);
        Long memberId = member.getId();
        redisTemplate.opsForValue().set("access_token:" + token, json, Duration.ofMinutes(30));
        String beforeToken = redisTemplate.opsForValue().get("member_id:" + memberId);
        if (beforeToken != null) {
            redisTemplate.delete("access_token:"+beforeToken);
        }
        redisTemplate.opsForValue().set("member_id:" + memberId, token, Duration.ofMinutes(30));
    }

    public LoginMemberDto get(String token) {
        String json = redisTemplate.opsForValue().get("access_token:" + token);
        if (json == null) return null;
        return gson.fromJson(json, LoginMemberDto.class);
    }
}
