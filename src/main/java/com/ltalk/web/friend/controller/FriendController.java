package com.ltalk.web.friend.controller;

import com.ltalk.web.friend.domain.Friend;
import com.ltalk.web.friend.dto.request.FriendRequest;
import com.ltalk.web.friend.dto.request.UpdateFriendRequest;
import com.ltalk.web.friend.dto.response.FriendListResponse;
import com.ltalk.web.friend.dto.response.FriendRequestListResponse;
import com.ltalk.web.friend.dto.response.RequestFriendResponse;
import com.ltalk.web.friend.service.FriendService;
import com.ltalk.web.global.config.LoginMember;
import com.ltalk.web.global.dto.LoginMemberDto;

import com.ltalk.web.member.domain.Member;
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

    @PatchMapping("/{friendId}/accept")
    @ResponseBody
    public ResponseEntity<List<Friend>> acceptFriendRequest(@LoginMember Member member, @PathVariable Long friendId){
       List<Friend> friendList = friendService.acceptFriendRequest(member, friendId);
       return ResponseEntity.ok(friendList);
    }

    @DeleteMapping("/{friendId}")
    @ResponseBody
    public ResponseEntity<List<Friend>> deleteFriend(@LoginMember Member member, @PathVariable Long friendId){
        List<Friend> friendList = friendService.deleteFriend(member, friendId);
        return ResponseEntity.ok(friendList);
    }

}
