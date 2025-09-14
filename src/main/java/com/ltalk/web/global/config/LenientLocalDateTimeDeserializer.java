package com.ltalk.web.global.config;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter SPACE = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            // 소수부 0~9자리 허용 (마이크로/나노 초 가변 대응)
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .toFormatter();

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 문자열만 처리. 다른 타입이면 컨텍스트에 위임
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            String s = p.getText(); // <-- getValueAsString() 대신 getText() 사용
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;

            // 'T' 있으면 ISO, 없으면 공백 포맷
            if (s.length() > 10 && s.charAt(10) == 'T') {
                return LocalDateTime.parse(s, ISO);
            }
            return LocalDateTime.parse(s, SPACE);
        }
        // 혹시 숫자(에폭) 등 다른 형태면 Jackson 기본 처리 시도
        return ctxt.readValue(p, LocalDateTime.class);
    }
}
