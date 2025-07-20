package com.ltalk.web.chatroom.repository;

import com.ltalk.web.chatroom.domain.ChatRoom;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("""
SELECT DISTINCT cr
FROM ChatRoom cr
JOIN FETCH cr.chatList
JOIN FETCH cr.memberList
WHERE cr.id IN :ids
""")
    List<ChatRoom> findChatRoomsWithAllByIds(@Param("ids") List<Long> ids);
    @Query("""
SELECT DISTINCT cr
FROM ChatRoom cr
LEFT JOIN FETCH cr.chatList
LEFT JOIN FETCH cr.memberList
WHERE cr.id IN :ids
""")
    List<ChatRoom> findChatRoomsWithChatsAndMembers(@Param("ids") List<Long> ids);


}

