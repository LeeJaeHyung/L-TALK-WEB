package com.ltalk.web.chatroom.controller;

import com.ltalk.web.chatroom.dto.request.ChatRoomCreateRequest;
import com.ltalk.web.chatroom.service.ChatRoomService;
import com.ltalk.web.global.dto.LoginMemberDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@AllArgsConstructor
@RequestMapping("/chatrooms")
public class ChatRoomController {

    private ChatRoomService chatRoomService;

    @ResponseBody
    @PostMapping
    public void createChatRoom(HttpServletRequest request, @RequestBody ChatRoomCreateRequest chatRoomCreateRequest) {
        chatRoomService.createChatRoom((LoginMemberDto) request.getAttribute("member"), chatRoomCreateRequest);
    }
}
