package com.ltalk.web.friend.repository;


import com.ltalk.web.friend.domain.Friend;
import com.ltalk.web.friend.domain.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByFromMemberIdOrToMemberIdAndStatus(Long fromMember_id, Long toMember_id, FriendStatus status);
    List<Friend> findByFromMemberIdAndStatus(Long memberid, FriendStatus friendStatus);
    List<Friend> findByToMemberIdAndStatus(Long memberid, FriendStatus friendStatus);
    boolean existsByFromMemberIdAndToMemberIdOrToMemberIdAndFromMemberId(
            Long fromMemberId1, Long toMemberId1,
            Long fromMemberId2, Long toMemberId2
    );


}
