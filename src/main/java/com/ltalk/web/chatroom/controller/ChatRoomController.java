package com.ltalk.web.chatroom.controller;

import com.ltalk.web.chatroom.domain.ChatRoom;
import com.ltalk.web.chatroom.dto.request.ChatRoomCreateRequest;
import com.ltalk.web.chatroom.dto.request.ChatRoomExitRequest;
import com.ltalk.web.chatroom.dto.response.ChatRoomDto;
import com.ltalk.web.chatroom.service.ChatRoomService;
import com.ltalk.web.global.config.LoginMember;
import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @ResponseBody
    @GetMapping
    public ResponseEntity<List<ChatRoomDto>> getChatRooms(@LoginMember Member member) {
        return ResponseEntity.ok(chatRoomService.getChatRoomsForMember(member.getId()));
    }

    @ResponseBody
    @DeleteMapping
    public ResponseEntity deleteChatRoom(@LoginMember Member member, @RequestBody ChatRoomExitRequest chatRoomExitRequest) {
        chatRoomService.exitChatRoom(member, chatRoomExitRequest);
        return ResponseEntity.ok().build();
    }
}
