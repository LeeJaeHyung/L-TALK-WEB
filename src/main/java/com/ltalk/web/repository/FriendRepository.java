package com.ltalk.web.repository;

import com.ltalk.web.entity.Friend;
import com.ltalk.web.entity.Member;
import com.ltalk.web.enums.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByFromMemberIdOrToMemberIdAndStatus(Long fromMember_id, Long toMember_id, FriendStatus status);
    List<Friend> findByFromMemberIdAndStatus(Long memberid, FriendStatus friendStatus);
    List<Friend> findByToMemberIdAndStatus(Long memberid, FriendStatus friendStatus);
    boolean existsByFromMemberIdAndToMemberIdOrToMemberIdAndFromMemberId(
            Long fromMemberId1, Long toMemberId1,
            Long fromMemberId2, Long toMemberId2
    );


}
