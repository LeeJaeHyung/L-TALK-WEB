package com.ltalk.web;

import com.ltalk.web.chatroom.domain.ChatRoomMember;
import com.ltalk.web.chatroom.repository.ChatRoomMemberRepository;
import com.ltalk.web.member.domain.Member;
import com.ltalk.web.member.domain.UserRole;
import com.ltalk.web.member.repository.MemberRepository;
import com.ltalk.web.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LTalkWebApplicationTests {
    @Autowired
    MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Test
    void addBlankUser() {
//        Member member = new Member("알 수 없는 사용자", "알 수 없는 사용자", "알 수 없는 사용자", "알 수 없는 사용자", "알 수 없는 사용자", UserRole.UNKNOWN_USER);
//        memberRepository.save(member);
        Member member = memberRepository.findByUserName("알 수 없는 사용자").orElseThrow();
        ChatRoomMember  chatRoomMember  = new ChatRoomMember(member);
        chatRoomMemberRepository.save(chatRoomMember);
    }

}
