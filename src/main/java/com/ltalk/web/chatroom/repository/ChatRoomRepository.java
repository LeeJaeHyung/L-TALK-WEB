package com.ltalk.web.chatroom.repository;

import com.ltalk.web.chat.dto.response.ChatRoomSummaryProjection;
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

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        LEFT JOIN FETCH cr.memberList
        WHERE cr.id IN :ids
    """)
    List<ChatRoom> findChatRoomsWithMembersOnly(@Param("ids") List<Long> ids);

    @Query(value = """
    WITH latest_chat AS (
      SELECT c.id,
             s.chat_room_id,
             c.message,
             c.created_at,
             s.id AS sender_id,
             ROW_NUMBER() OVER (PARTITION BY s.chat_room_id ORDER BY c.created_at DESC, c.id DESC) AS rn
      FROM chat c
      JOIN chat_room_member s ON s.id = c.sender_id AND s.deleted = 0
      WHERE c.deleted = 0
    )
    SELECT
      cr.id AS chatRoomId,
      cr.name AS chatRoomName,
      cr.participant_count AS participantCount,

      /* 방 멤버 JSON 집계: DTO(ChatRoomMemberDto)의 필드명과 1:1 매칭 */
      JSON_ARRAYAGG(
        JSON_OBJECT(
          'id',        ml.id,          -- 기존 'crmId' -> 'id'
          'memberId',  ml.member_id,
          'username',  m.user_name     -- 기존 'userName' -> 'username'
        )
      ) AS membersJson,

      /* 최신 채팅 JSON (방마다 1건, DTO(ChatDto) 필드명과 1:1 매칭) */
      (
        SELECT JSON_OBJECT(
          'id',         lc.id,
          'message',    lc.message,
          'createdAt',  lc.created_at,
          'senderId',   lc.sender_id,
          'chatRoomId', cr.id
        )
        FROM latest_chat lc
        WHERE lc.chat_room_id = cr.id AND lc.rn = 1
      ) AS lastChatJson

    FROM chat_room cr
    JOIN chat_room_member my_crm
      ON my_crm.chat_room_id = cr.id AND my_crm.deleted = 0
    JOIN chat_room_member ml
      ON ml.chat_room_id = cr.id AND ml.deleted = 0
    JOIN member m
      ON m.id = ml.member_id AND m.deleted_at IS NULL
    WHERE my_crm.member_id = :memberId
    GROUP BY cr.id, cr.name, cr.participant_count
    """, nativeQuery = true)
    List<ChatRoomSummaryProjection> findChatRoomSummaries(@Param("memberId") Long memberId);



}

