package com.ltalk.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranslatorService {

    @Value("${translator.endpoint}")
    private String endpoint;          // https://api.cognitive.microsofttranslator.com/

    @Value("${translator.key}")
    private String subscriptionKey;   // Key1 or Key2

    @Value("${translator.region}")
    private String region;            // koreacentral

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param text 번역할 텍스트
     * @param from 원본 언어 코드 (nullable, null이면 자동 감지)
     * @param to   목표 언어 코드 (예: ko, en, ja, zh-Hans 등)
     */
    public String translate(String text, String from, String to) {
        try {
            if (text == null || text.isBlank()) {
                return "";
            }
            if (to == null || to.isBlank()) {
                to = "ko";
            }

            // endpoint 뒤에 /가 있어도, 없어도 안전하게 처리
            String base = endpoint;
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }

            String path = "/translate";
            String params = "?api-version=3.0"
                    + (from != null && !from.isBlank()
                    ? "&from=" + URLEncoder.encode(from, StandardCharsets.UTF_8)
                    : "")
                    + "&to=" + URLEncoder.encode(to, StandardCharsets.UTF_8);

            String url = base + path + params;

            // 요청 바디 – 여러 문장도 가능하지만 여기선 단일 텍스트만
            List<Map<String, String>> body = List.of(
                    Map.of("Text", text)
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
            headers.set("Ocp-Apim-Subscription-Region", region);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // 응답 형태:
                // [
                //   {
                //     "translations": [
                //       { "text": "...", "to": "..." }
                //     ]
                //   }
                // ]
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode first = root.get(0);
                JsonNode translations = first.get("translations");
                JsonNode firstTranslation = translations.get(0);
                return firstTranslation.get("text").asText();
            } else {
                throw new RuntimeException("Translator API 호출 실패: " + response.getStatusCode());
            }

        } catch (Exception e) {
            // 로그 찍고 싶으면 Logger 써도 됨
            throw new RuntimeException("Translator API 오류", e);
        }
    }

    /**
     * 언어 자동 감지 + 번역 (from 생략용 헬퍼)
     */
    public String translateAutoDetect(String text, String to) {
        return translate(text, null, to);
    }
}
