package com.ltalk.web.chatroom.repository;

import com.ltalk.web.chatroom.domain.ChatRoomMember;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByMemberIdAndChatRoomId(Long member_id, Long chatRoom_id);

    List<ChatRoomMember> findAllByMemberId(Long member_id);
    @Query("""
    SELECT crm FROM ChatRoomMember crm
    WHERE crm.member.id = :memberId
""")
    List<ChatRoomMember> findAllFromMemberId(@Param("memberId") Long memberId);

    Optional<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    boolean existsByMemberIdAndChatRoomId(Long memberId, Long chatRoomId);
}
