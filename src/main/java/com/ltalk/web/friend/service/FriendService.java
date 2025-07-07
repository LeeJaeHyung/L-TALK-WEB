package com.ltalk.web.friend.service;

import com.ltalk.web.friend.domain.Friend;
import com.ltalk.web.friend.domain.FriendStatus;
import com.ltalk.web.friend.dto.request.UpdateFriendRequest;
import com.ltalk.web.friend.dto.response.FriendListResponse;
import com.ltalk.web.friend.dto.response.FriendRequestListResponse;
import com.ltalk.web.friend.dto.response.RequestFriendResponse;
import com.ltalk.web.friend.repository.FriendRepository;
import com.ltalk.web.global.dto.LoginMemberDto;

import com.ltalk.web.member.domain.Member;
import com.ltalk.web.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
        List<Friend> friendList = friendRepository.findAcceptedFriends(memberId, FriendStatus.ACCEPTED);
        return new FriendListResponse(friendList);
    }

    @Transactional
    public RequestFriendResponse requestFriend(Long fromMemberId, Long toMemberId) {
        Friend responseFriend = null;
        //이미 친구 상태인지 확인
        if(!friendRepository.existsFriendRelation(fromMemberId, toMemberId)){
            Member fromMember = memberRepository.findById(fromMemberId).orElseThrow();
            System.out.println("fromMember 조회");
            Member toMember = memberRepository.findById(toMemberId).orElseThrow(()-> new IllegalArgumentException("존재하지 않는 멤버에게 요청을 보냈습니다."));
            System.out.println("toMember 조회");
            Friend friend = new Friend(fromMember, toMember, FriendStatus.REQUESTED);
            return new RequestFriendResponse(friendRepository.save(friend));
        }
        throw new IllegalArgumentException("친구요청을 신청할 수 없는 상태 입니다.");
    }

    public FriendRequestListResponse getRequestFriendList(Long memberid) {
        List<Friend> myRequestList = friendRepository.findByFromMemberIdAndStatus(memberid, FriendStatus.REQUESTED);
        List<Friend> requestByOtherList = friendRepository.findByToMemberIdAndStatus(memberid, FriendStatus.REQUESTED);
        return new FriendRequestListResponse(myRequestList, requestByOtherList);
    }

    @Transactional
    public List<Friend> acceptFriendRequest(Member member, Long friendId) {
        Friend friend = friendRepository.findById(friendId).orElseThrow(()-> new IllegalArgumentException("존재하지 않는 데이터입니다."));
        Long memberId = member.getId();
        if(friend.getStatus()==FriendStatus.REQUESTED&&friend.getToMember().getId().equals(memberId)){
            friend.setStatus(FriendStatus.ACCEPTED);
            return friendRepository.findAcceptedFriends(memberId, FriendStatus.ACCEPTED);
        }
        throw new IllegalArgumentException("접근 가능한 데이터가 아닙니다.");
    }

    @Transactional
    public List<Friend> deleteFriend(Member member, Long friendId) {
        Long memberId = member.getId();
        Friend friend = friendRepository.findById(friendId).orElseThrow(()-> new IllegalArgumentException("존재 하지 않는 데이터입니다."));
        if(friend.getFromMember().getId().equals(memberId)||friend.getToMember().getId().equals(memberId)){
            friend.setStatus(FriendStatus.DELETED);
            friend.softDelete();
        }
        return friendRepository.findAcceptedFriends(memberId, FriendStatus.ACCEPTED);
    }
}
