package com.ltalk.web.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/translate")
public class TranslatorController {

    private final TranslatorService translatorService;

    /**
     * 예:
     * GET /api/translate?text=안녕하세요&to=en
     * GET /api/translate?text=你好&from=zh-Hans&to=ko
     */
    @GetMapping
    public String translate(
            @RequestParam String text,
            @RequestParam(defaultValue = "en") String to,
            @RequestParam(required = false) String from
    ) {
        return translatorService.translate(text, from, to);
    }

    @PostMapping
    public String translate(@RequestBody TranslatorRequest request) {
        return translatorService.translate(request.getContent(), request.getFrom(), request.getTo());
    }
}
