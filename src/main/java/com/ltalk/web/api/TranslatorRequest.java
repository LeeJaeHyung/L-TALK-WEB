package com.ltalk.web.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslatorRequest {
    String to;
    String from;
    String content;
}
