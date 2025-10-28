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
import com.ltalk.web.chatroom.dto.response.ChatRoomViewDto;
import com.ltalk.web.chatroom.dto.response.ChatViewDto;
import com.ltalk.web.chatroom.util.JsonParserUtil;
import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.member.domain.Member;
import com.ltalk.web.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ChatRoomService {

    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRepository chatRepository;
    private final JsonParserUtil jsonParserUtil;


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
    public List<ChatRoomDto> getChatRoomsForMember1(Long memberId) {
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
            List<Chat> chats = chatRoom.getChatList();
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

    public boolean canSubscribe(Long memberId, Long roomId){
        return chatRoomMemberRepository.existsByMemberIdAndChatRoomId(memberId, roomId);
    }



    public List<ChatRoomDto> getChatRoomsForMember(Long memberId) {
        // 1) 내가 속한 방 ID 수집
        List<ChatRoomMember> chatRoomMemberList = chatRoomMemberRepository.findAllFromMemberId(memberId);
        List<Long> roomIds = chatRoomMemberList.stream()
                .map(crm -> crm.getChatRoom().getId())
                .toList();

        if (roomIds.isEmpty()) return List.of();

        // 2) 방 + 멤버만 페치
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsWithMembersOnly(roomIds);

        // 3) 방별 최신 채팅 1건만 조회
        List<Chat> latestChats = chatRepository.findLatestChatByRoomIds(roomIds);

        // sender.chatRoom.id 기준으로 매핑
        Map<Long, Chat> latestByRoomId = latestChats.stream()
                .collect(Collectors.toMap(
                        c -> c.getSender().getChatRoom().getId(),
                        c -> c
                ));

        // 4) DTO로 변환 (chatList는 0~1건)
        return chatRooms.stream()
                .map(cr -> {
                    // 최신 채팅 1건을 DTO 리스트로
                    Chat latest = latestByRoomId.get(cr.getId());
                    List<ChatDto> chatDtoList = (latest == null)
                            ? List.of()
                            : List.of(new ChatDto(
                            latest.getId(),
                            latest.getSender().getMember().getId(),
                            latest.getSender().getChatRoom().getId(),
                            latest.getMessage(),
                            latest.getCreatedAt()
                    ));

                    return new ChatRoomDto(
                            cr.getId(),
                            cr.getName(),
                            cr.getType().name(),
                            cr.getParticipantCount(),
                            cr.getMemberList().stream()
                                    .map(m -> new ChatRoomMemberDto(
                                            m.getId(),
                                            m.getMember().getId(),
                                            m.getMember().getUserName()
                                    )).toList(),
                            chatDtoList
                    );
                })
                .toList();
    }

    public List<ChatRoomDto> getChatRoomsOneShot(Long memberId) {
        var rows = chatRoomRepository.findChatRoomSummaries(memberId);
        return rows.stream().map(r -> {
            List<ChatRoomMemberDto> members = jsonParserUtil.parseMembersJson(r.getMembersJson());
            ChatDto last = jsonParserUtil.parseLastChatJson(r.getLastChatJson()); // null 허용
            List<ChatDto> chats = (last == null) ? List.of() : List.of(last);
            return new ChatRoomDto(
                    r.getChatRoomId(),
                    r.getChatRoomName(),
                    r.getChatRoomType(), // 필요시 SELECT에 추가
                    r.getParticipantCount(),
                    members,
                    chats
            );
        }).toList();
    }

    public ChatRoomDto getChatRoom(Member member, Long roomId) {
        if(!chatRoomMemberRepository.existsByMemberIdAndChatRoomId(member.getId(), roomId)){
            new IllegalArgumentException("해당 채팅방에 접근할 권한이 없습니다");
        }
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembersAndChats(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        return new ChatRoomDto(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getType().name(),
                chatRoom.getParticipantCount(),
                chatRoom.getMemberList().stream()
                        .map(m -> new ChatRoomMemberDto(
                                m.getId(),
                                m.getMember().getId(),
                                m.getMember().getUserName()
                        ))
                        .toList(),
                chatRoom.getChatList().stream()
                        .map(c -> new ChatDto(
                                c.getId(),
                                c.getSender().getMember().getId(),
                                c.getSender().getChatRoom().getId(),
                                c.getMessage(),
                                c.getCreatedAt()
                        ))
                        .toList()
        );
    }

    @Transactional
    public ChatRoomViewDto getChatRoomView(Member member, Long roomId) {
        if (!chatRoomMemberRepository.existsByMemberIdAndChatRoomId(member.getId(), roomId)) {
            throw new IllegalArgumentException("해당 채팅방에 접근할 권한이 없습니다");
        }

        ChatRoom room = chatRoomRepository.findRoomHeaderById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        List<ChatRoomMemberDto> members = chatRoomRepository.findMemberDtosByChatRoomId(roomId);
        Pageable pageable = PageRequest.of(0, 50);
        Page<ChatViewDto> chats = chatRoomRepository.findChatViewsByChatRoomId(roomId, pageable);

        return new ChatRoomViewDto(
                room.getId(),
                room.getName(),
                room.getType() != null ? room.getType().name() : null,
                room.getParticipantCount(),
                members,
                chats.stream().toList()
        );
    }
}
