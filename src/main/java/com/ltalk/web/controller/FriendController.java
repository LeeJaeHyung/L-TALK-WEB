package com.ltalk.web.controller;

import com.ltalk.web.dto.LoginMemberDto;
import com.ltalk.web.dto.request.FriendRequest;
import com.ltalk.web.dto.response.FriendListResponse;
import com.ltalk.web.dto.response.FriendRequestListResponse;
import com.ltalk.web.dto.response.RequestFriendResponse;
import com.ltalk.web.entity.Friend;
import com.ltalk.web.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/friends")
public class FriendController {

   private FriendService friendService;

   public FriendController(FriendService friendService) {
       this.friendService = friendService;
   }


    @GetMapping("")
    @ResponseBody
    public ResponseEntity<FriendListResponse> getFriendList(HttpServletRequest request){
        return ResponseEntity.ok(friendService.getFriendList((LoginMemberDto)request.getAttribute("member")));
    }

    @GetMapping("/requests")
    @ResponseBody
    public ResponseEntity<FriendRequestListResponse> getRequestFriendList(HttpServletRequest request){
       return ResponseEntity.ok(friendService.getRequestFriendList(((LoginMemberDto)request.getAttribute("member")).getId()));
    }

    @PostMapping("")
    @ResponseBody
    public ResponseEntity<RequestFriendResponse> requestFriend(HttpServletRequest request, @RequestBody FriendRequest friendRequest){
       System.out.println("/friends  Post방식 접근");
       return ResponseEntity.ok(friendService.requestFriend(((LoginMemberDto)request.getAttribute("member")).getId(), friendRequest.getToMemberId()));
    }

}
