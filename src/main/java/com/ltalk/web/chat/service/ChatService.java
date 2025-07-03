package com.ltalk.web.chat.service;

import com.ltalk.web.chat.domain.Chat;
import com.ltalk.web.chat.dto.request.ChatCreateRequest;
import com.ltalk.web.chat.repository.ChatRepository;
import com.ltalk.web.chatroom.domain.ChatRoom;
import com.ltalk.web.chatroom.domain.ChatRoomMember;
import com.ltalk.web.chatroom.repository.ChatRoomMemberRepository;
import com.ltalk.web.chatroom.repository.ChatRoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
}
