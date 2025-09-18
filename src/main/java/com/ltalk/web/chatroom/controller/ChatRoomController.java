package com.ltalk.web.chatroom.controller;

import com.ltalk.web.chatroom.dto.request.ChatRoomCreateRequest;
import com.ltalk.web.chatroom.dto.request.ChatRoomExitRequest;
import com.ltalk.web.chatroom.dto.response.ChatRoomDto;
import com.ltalk.web.chatroom.dto.response.ChatRoomViewDto;
import com.ltalk.web.chatroom.service.ChatRoomService;
import com.ltalk.web.global.config.LoginMember;
import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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
        ResponseEntity<List<ChatRoomDto>> x =  ResponseEntity.ok(chatRoomService.getChatRoomsOneShot(member.getId()));
        for(ChatRoomDto dto : x.getBody()) {
            System.out.println(dto.type());
        }
        return x;
    }

    @ResponseBody
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ChatRoomViewDto> getChatRoom(@LoginMember Member member, @PathVariable Long chatRoomId) {
        System.out.println("=========================================================================================");
        Date startDate = new Date(System.currentTimeMillis());
        System.out.println("시작 시간 : "+startDate.getTime());
        var x =  ResponseEntity.ok(chatRoomService.getChatRoomView(member, chatRoomId));
        Date endDate = new Date(System.currentTimeMillis());
        System.out.println("종료 시간 :"+endDate.getTime());
        System.out.println((endDate.getTime()-startDate.getTime())+"ms 걸림 ");
        System.out.println("=========================================================================================");
        return x;
    }

    @ResponseBody
    @DeleteMapping
    public ResponseEntity deleteChatRoom(@LoginMember Member member, @RequestBody ChatRoomExitRequest chatRoomExitRequest) {
        chatRoomService.exitChatRoom(member, chatRoomExitRequest);
        return ResponseEntity.ok().build();
    }
}
