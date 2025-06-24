package com.ltalk.web.service;

import com.ltalk.web.dto.LoginMemberDto;
import com.ltalk.web.dto.response.FriendListResponse;
import com.ltalk.web.dto.response.FriendRequestListResponse;
import com.ltalk.web.dto.response.RequestFriendResponse;
import com.ltalk.web.entity.Friend;
import com.ltalk.web.entity.Member;
import com.ltalk.web.enums.FriendStatus;
import com.ltalk.web.repository.FriendRepository;
import com.ltalk.web.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final MemberRepository memberRepository;

    public FriendService(FriendRepository friendRepository, MemberRepository memberRepository) {
        this.friendRepository = friendRepository;
        this.memberRepository = memberRepository;
    }

    public FriendListResponse getFriendList(LoginMemberDto loginMemberDto) {
        Long memberId = loginMemberDto.getId();
        List<Friend> friendList = friendRepository.findByFromMemberIdOrToMemberIdAndStatus(memberId,memberId, FriendStatus.ACCEPTED);
        return new FriendListResponse(friendList);
    }

    public RequestFriendResponse requestFriend(Long fromMemberId, Long toMemberId) {
        Member fromMember = memberRepository.findById(fromMemberId).orElseThrow();
        System.out.println("fromMember 조회");
        Member toMember = memberRepository.findById(toMemberId).orElseThrow();
        System.out.println("toMember 조회");
        Friend friend = new Friend(fromMember, toMember, FriendStatus.REQUESTED);
        return new RequestFriendResponse(friendRepository.save(friend));
    }

    public FriendRequestListResponse getRequestFriendList(Long memberid) {
        List<Friend> myRequestList = friendRepository.findByFromMemberIdAndStatus(memberid, FriendStatus.REQUESTED);
        List<Friend> requestByOtherList = friendRepository.findByToMemberIdAndStatus(memberid, FriendStatus.REQUESTED);
        return new FriendRequestListResponse(myRequestList, requestByOtherList);
    }
}
