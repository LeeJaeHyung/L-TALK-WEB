package com.ltalk.web.chatroom.service;

import com.ltalk.web.chat.domain.Chat;
import com.ltalk.web.chat.dto.response.ChatDto;
import com.ltalk.web.chat.repository.ChatRepository;
import com.ltalk.web.chatroom.domain.ChatRoom;
import com.ltalk.web.chatroom.domain.ChatRoomMember;
import com.ltalk.web.chatroom.dto.request.ChatRoomCreateRequest;
import com.ltalk.web.chatroom.dto.request.ChatRoomExitRequest;
import com.ltalk.web.chatroom.dto.response.ChatRoomDto;
import com.ltalk.web.chatroom.dto.response.ChatRoomMemberDto;
import com.ltalk.web.chatroom.repository.ChatRoomMemberRepository;
import com.ltalk.web.chatroom.repository.ChatRoomRepository;
import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.member.domain.Member;
import com.ltalk.web.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.type.LogicalType.Collection;

@Service
public class ChatRoomService {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private DataSourceTransactionManagerAutoConfiguration dataSourceTransactionManagerAutoConfiguration;

    @Transactional
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

        Set<ChatRoomMember> chatRoomMembers = new HashSet<>();
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

    @Transactional
    public List<ChatRoomDto> getChatRoomsForMember(Long memberId) {
        List<ChatRoomMember> chatRoomMemberList = chatRoomMemberRepository.findAllFromMemberId(memberId);
        System.out.println("chatRoomMemberList size : " + chatRoomMemberList.size());
        List<Long> ids = new ArrayList<>();
        for (ChatRoomMember crm : chatRoomMemberList) {
            Long id = crm.getChatRoom().getId();
            ids.add(id);
        }
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsWithChatsAndMembers(ids);
        System.out.println("chatRooms size : " + chatRooms.size());
        for (ChatRoom chatRoom : chatRooms) {
            System.out.println(chatRoom.getId());
            Set<Chat> chats = chatRoom.getChatList();
            for (Chat chat : chats) {
                System.out.println(chat.getMessage());
            }
        }
        List<ChatRoomDto> dtos = chatRooms.stream()
                .map(chatRoom -> new ChatRoomDto(
                        chatRoom.getId(),
                        chatRoom.getName(),
                        chatRoom.getType().name(),
                        chatRoom.getParticipantCount(),
                        chatRoom.getMemberList().stream()
                                .map(member -> new ChatRoomMemberDto(
                                        member.getId(),
                                        member.getMember().getId(),
                                        member.getMember().getUserName()
                                )).toList(),
                        chatRoom.getChatList().stream()
                                .map(chat -> new ChatDto(
                                        chat.getId(),
                                        chat.getSender().getMember().getId(),
                                        chat.getSender().getChatRoom().getId(),
                                        chat.getMessage(),
                                        chat.getCreatedAt()
                                )).toList()
                ))
                .toList();

        return dtos;
    }

    @Transactional
    public void deleteAllChatDataForMember(Long memberId) {
        List<ChatRoomMember> crmList = chatRoomMemberRepository.findAllByMemberId(memberId);
        for (ChatRoomMember crm : crmList) {
            crm.setDeleted(true);
        }

        List<Long> crmIds = crmList.stream().map(ChatRoomMember::getId).toList();
        List<Chat> chats = chatRepository.findAllBySender_IdIn(crmIds);
        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findById(12L).orElseThrow();
        for (Chat chat : chats) {
            chat.setSender(chatRoomMember);
        }
    }

    @Transactional
    public void exitChatRoom(Member member, ChatRoomExitRequest chatRoomExitRequest) {
        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByMemberIdAndChatRoomId(member.getId(), chatRoomExitRequest.getChatRoomId()).orElseThrow(()-> new IllegalArgumentException("일치하는 멤버가 없습니다."));
        chatRoomMember.setDeleted(true);
        ChatRoomMember blankMember = chatRoomMemberRepository.findByChatRoomId(null).orElseThrow();
        List<Chat> chats = chatRepository.findAllBySenderId(chatRoomMember.getId());
        for (Chat chat : chats) {
            chat.setSender(blankMember);
        }
    }
}
