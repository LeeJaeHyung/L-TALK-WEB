package com.ltalk.web.chat.repository;

import com.ltalk.web.chat.domain.Chat;
import com.ltalk.web.chatroom.dto.response.ChatViewDto;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
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


    // 최신 50개
    @Query("""
        select new com.ltalk.web.chatroom.dto.response.ChatViewDto(
            c.id, cr.id, m.nickName, c.message, c.createdAt
        )
        from Chat c
        join c.chatRoom cr
        join c.sender s
        join s.member m
        where cr.id = :roomId
        order by c.createdAt desc, c.id desc
    """)
    List<ChatViewDto> findLatestChats(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    // 커서 기반 과거 메시지
    @Query("""
        select new com.ltalk.web.chatroom.dto.response.ChatViewDto(
            c.id, cr.id, m.nickName, c.message, c.createdAt
        )
        from Chat c
        join c.chatRoom cr
        join c.sender s
        join s.member m
        where cr.id = :roomId
          and (
            c.createdAt < :cursorAt
            or (c.createdAt = :cursorAt and c.id < :cursorId)
          )
        order by c.createdAt desc, c.id desc
    """)
    List<ChatViewDto> findOlderThanCursor(
            @Param("roomId") Long roomId,
            @Param("cursorAt") LocalDateTime cursorAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
