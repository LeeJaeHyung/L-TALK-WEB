package com.ltalk.web.chatroom.util;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ltalk.web.chat.dto.response.ChatDto;
import com.ltalk.web.chatroom.dto.response.ChatRoomMemberDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JsonParserUtil {

    private final ObjectMapper om;

    public JsonParserUtil(ObjectMapper objectMapper) {
        this.om = objectMapper; // 스프링이 자동 구성한 Mapper (JavaTimeModule 포함)
        this.om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<ChatRoomMemberDto> parseMembersJson(String json) {
        if (json == null) return List.of();
        try {
            return om.readValue(json, new TypeReference<List<ChatRoomMemberDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse membersJson", e);
        }
    }

    public ChatDto parseLastChatJson(String json) {
        if (json == null) return null;
        try {
            return om.readValue(json, ChatDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse lastChatJson", e);
        }
    }
}