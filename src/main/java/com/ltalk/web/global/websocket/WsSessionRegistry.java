package com.ltalk.web.global.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WsSessionRegistry {
    private final ConcurrentMap<Object, Set<WebSocketSession>> byUser  = new ConcurrentHashMap<>();
    private final ConcurrentMap<Object, Set<WebSocketSession>> byToken = new ConcurrentHashMap<>();

    public void register(Object userId, Object token, WebSocketSession s) {
        if (userId != null) byUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(s);
        if (token  != null) byToken.computeIfAbsent(token,  k -> ConcurrentHashMap.newKeySet()).add(s);
    }
    public void unregister(Object userId, Object token, WebSocketSession s) {
        if (userId != null) removeFrom(byUser, userId, s);
        if (token  != null) removeFrom(byToken, token,  s);
    }
    private void removeFrom(Map<Object, Set<WebSocketSession>> map, Object key, WebSocketSession s){
        var set = map.get(key);
        if (set != null) {
            set.remove(s);
            if (set.isEmpty()) map.remove(key);
        }
    }

    public void disconnectByUser(Object userId, CloseStatus status) {
        closeAll(byUser.remove(userId), status);
    }
    public void disconnectByToken(Object token, CloseStatus status) {
        closeAll(byToken.remove(token), status);
    }
    private void closeAll(Set<WebSocketSession> set, CloseStatus status) {
        if (set == null) return;
        for (var s : set) try { s.close(status != null ? status : CloseStatus.POLICY_VIOLATION); } catch (Exception ignore) {}
    }
}
