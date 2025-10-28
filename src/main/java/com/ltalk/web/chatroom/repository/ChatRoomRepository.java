package com.ltalk.web.chatroom.repository;

import com.ltalk.web.chat.dto.response.ChatRoomSummaryProjection;
import com.ltalk.web.chatroom.domain.ChatRoom;
import com.ltalk.web.chatroom.dto.response.ChatViewDto;
import com.ltalk.web.chatroom.dto.response.ChatRoomMemberDto;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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
      cr.type AS chatRoomType,
      cr.participant_count AS participantCount,

      /* 멤버 JSON: 일관 정렬 보장 */
      JSON_ARRAYAGG(
        JSON_OBJECT(
          'id',       ml.id,
          'memberId', ml.member_id,
          'username', m.user_name
        )
      ) AS membersJson,

      /* 최신 채팅 JSON: 닉네임 포함 */
      (
        SELECT JSON_OBJECT(
          'id',              lc.id,
          'message',         lc.message,
          'createdAt',       lc.created_at,
          'senderId',        lc.sender_id,
          'senderNickname',  (
                               SELECT mm.nick_name
                               FROM chat_room_member s2
                               JOIN member mm ON mm.id = s2.member_id
                               WHERE s2.id = lc.sender_id
                               LIMIT 1
                             ),
          'chatRoomId',      cr.id
        )
        FROM latest_chat lc
        WHERE lc.chat_room_id = cr.id AND lc.rn = 1
      ) AS lastChatJson

    FROM chat_room cr
    JOIN chat_room_member my_crm
      ON my_crm.chat_room_id = cr.id AND my_crm.deleted = 0
    LEFT JOIN chat_room_member ml
      ON ml.chat_room_id = cr.id AND ml.deleted = 0
    LEFT JOIN member m
      ON m.id = ml.member_id AND m.deleted_at IS NULL
    WHERE my_crm.member_id = :memberId
    GROUP BY cr.id, cr.name, cr.type, cr.participant_count
    ORDER BY cr.id DESC
    """, nativeQuery = true)
    List<ChatRoomSummaryProjection> findChatRoomSummaries(@Param("memberId") Long memberId);

    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        LEFT JOIN FETCH cr.memberList m
        LEFT JOIN FETCH cr.chatList c
        WHERE cr.id = :chatRoomId
        order by c.createdAt
        """)
    Optional<ChatRoom> findByIdWithMembersAndChats(@Param("chatRoomId") Long chatRoomId);

    @Query("""
    select new com.ltalk.web.chatroom.dto.response.ChatViewDto(
        c.id,
        cr.id,
        m.nickName,
        c.message,
        c.createdAt
    )
    from Chat c
    join c.chatRoom cr
    join c.sender s
    join s.member m
    where cr.id = :chatRoomId
    order by c.createdAt desc, c.id desc
""")
    Page<ChatViewDto> findChatViewsByChatRoomId(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable
    );


    @Query("""
        select new com.ltalk.web.chatroom.dto.response.ChatRoomMemberDto(
            s.id,
            m.id,
            m.userName
        )
        from ChatRoomMember s
        join s.chatRoom cr
        join s.member m
        where cr.id = :chatRoomId and s.deleted = false and m.deletedAt is null
        order by s.id asc
    """)
    List<ChatRoomMemberDto> findMemberDtosByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("""
        select cr
        from ChatRoom cr
        where cr.id = :chatRoomId
    """)
    Optional<ChatRoom> findRoomHeaderById(@Param("chatRoomId") Long chatRoomId);

}

