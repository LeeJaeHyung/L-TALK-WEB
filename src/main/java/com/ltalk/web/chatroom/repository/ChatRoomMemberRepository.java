package com.ltalk.web.chatroom.repository;

import com.ltalk.web.chatroom.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByMemberIdAndChatRoomId(Long member_id, Long chatRoom_id);
}
