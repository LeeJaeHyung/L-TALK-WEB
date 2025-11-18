package com.ltalk.web.chat.service;

import com.ltalk.web.chat.domain.Chat;
import com.ltalk.web.chat.dto.request.ChatCreateRequest;
import com.ltalk.web.chat.dto.response.ChatSliceResponse;
import com.ltalk.web.chat.repository.ChatRepository;
import com.ltalk.web.chatroom.domain.ChatRoom;
import com.ltalk.web.chatroom.domain.ChatRoomMember;
import com.ltalk.web.chatroom.dto.response.ChatViewDto;
import com.ltalk.web.chatroom.repository.ChatRoomMemberRepository;
import com.ltalk.web.chatroom.repository.ChatRoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRepository chatRepository;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatRoomMemberRepository chatRoomMemberRepository, ChatRepository chatRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.chatRepository = chatRepository;
    }

    @Transactional
    public void createChat(Long chatRoomId, ChatCreateRequest request, Long senderId) {
        System.out.println("chatRoomId = " + chatRoomId+"     senderId = " + senderId);
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다"));

        ChatRoomMember sender = chatRoomMemberRepository.findByMemberIdAndChatRoomId(senderId, chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다"));

        Chat chat = new Chat();
        chat.setChatRoom(chatRoom);
        chat.setSender(sender);
        chat.setMessage(request.getMessage());
        chatRepository.save(chat);
        sender.setReadChatId(chat.getId());
        chatRoomMemberRepository.save(sender);
    }

    public ChatSliceResponse getChatSlice(Long roomId, LocalDateTime cursorAt, Long cursorId, int size) {
        List<ChatViewDto> chats;

        Pageable pageable = PageRequest.of(0, size + 1); // hasNext 판별용 +1개

        if (cursorAt == null || cursorId == null) {
            // 커서 없으면 최신 50개
            chats = chatRepository.findLatestChats(roomId, pageable);
        } else {
            // 커서 기반 과거 데이터 조회
            chats = chatRepository.findOlderThanCursor(roomId, cursorAt, cursorId, pageable);
        }

        boolean hasNext = chats.size() > size;
        if (hasNext) chats = chats.subList(0, size);

        ChatViewDto last = chats.isEmpty() ? null : chats.get(chats.size() - 1);

        return new ChatSliceResponse(
                chats,
                hasNext,
                last != null ? last.createdAt() : null,
                last != null ? last.id() : null
        );

    }
}
