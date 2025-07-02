package com.ltalk.web.chatroom.service;

import com.ltalk.web.chatroom.domain.ChatRoom;
import com.ltalk.web.chatroom.domain.ChatRoomMember;
import com.ltalk.web.chatroom.dto.request.ChatRoomCreateRequest;
import com.ltalk.web.chatroom.repository.ChatRoomRepository;
import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.member.domain.Member;
import com.ltalk.web.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    public void createChatRoom(LoginMemberDto member, ChatRoomCreateRequest chatRoomCreateRequest) {
        // 1. 참여자 ID 리스트 가져오기
        List<Long> participantIds = new ArrayList<>(chatRoomCreateRequest.getMemberIds());

        // 2. 본인 ID 추가 (로그인한 사용자)
        participantIds.add(member.getId());

        // 3. 중복 제거 (혹시 본인이 이미 포함돼 있을 수도 있으니까)
        List<Long> distinctIds = participantIds.stream()
                .distinct()
                .collect(Collectors.toList());

        // 4. DB에서 한 번에 조회

        List<Member> members = memberRepository.findAllById(distinctIds);

        // 5. 참여 인원 확인
        if (members.size() != distinctIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 회원이 포함되어 있습니다.");
        }

        List<ChatRoomMember> chatRoomMembers = new ArrayList<>();
        for(Member member1 : members) {
            chatRoomMembers.add(new ChatRoomMember(member1));
        }

        // 6. ChatRoom 생성
        ChatRoom chatRoom = new ChatRoom(chatRoomCreateRequest, chatRoomMembers);

        for (ChatRoomMember crm : chatRoomMembers) {
            crm.setChatRoom(chatRoom);
        }

        // 7. 저장
        chatRoomRepository.save(chatRoom);
    }
}
