package com.ltalk.web.controller;

import com.ltalk.web.entity.ChatRoom;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chatrooms")
public class ChatRoomController {

    @PostMapping
    public void createChatRoom() {

    }
}
