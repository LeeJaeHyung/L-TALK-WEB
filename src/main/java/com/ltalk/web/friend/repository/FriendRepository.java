package com.ltalk.web.friend.repository;


import com.ltalk.web.friend.domain.Friend;
import com.ltalk.web.friend.domain.FriendStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface FriendRepository extends JpaRepository<Friend, Long> {
    @Query("SELECT f FROM Friend f WHERE (f.fromMember.id = :memberId OR f.toMember.id = :memberId) AND f.status = :status")
    List<Friend> findAcceptedFriends(@Param("memberId") Long memberId, @Param("status") FriendStatus status);

    List<Friend> findByFromMemberIdAndStatus(Long memberid, FriendStatus friendStatus);

    List<Friend> findByToMemberIdAndStatus(Long memberid, FriendStatus friendStatus);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friend f WHERE (f.fromMember.id = :memberId1 AND f.toMember.id = :memberId2) OR (f.fromMember.id = :memberId2 AND f.toMember.id = :memberId1)")
    boolean existsFriendRelation(@Param("memberId1") Long memberId1, @Param("memberId2") Long memberId2);

}
