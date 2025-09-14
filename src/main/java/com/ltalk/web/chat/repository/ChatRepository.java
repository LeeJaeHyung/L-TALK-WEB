package com.ltalk.web.chat.repository;

import com.ltalk.web.chat.domain.Chat;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllBySender_IdIn(List<Long> senderIds);

    List<Chat> findAllBySenderId(Long senderId);

    @Query("""
        SELECT c
        FROM Chat c
        JOIN c.sender s
        JOIN s.chatRoom r
        WHERE r.id IN :roomIds
          AND c.createdAt = (
              SELECT MAX(c2.createdAt)
              FROM Chat c2
              JOIN c2.sender s2
              WHERE s2.chatRoom = r
          )
    """)
    List<Chat> findLatestChatByRoomIds(@Param("roomIds") List<Long> roomIds);
}
