package com.ltalk.web.repository;

import com.ltalk.web.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 사용자 이름으로 검색하는 메서드 (로그인 등에 사용 가능)
    Optional<Member> findByUserName(String userName);

    boolean existsByUserNameAndNickNameAndEmail(String userName, String nickName, String email);

    // 이메일 중복 확인 등에도 활용 가능
    boolean existsByEmail(String email);

    // 닉네임 중복 확인
    boolean existsByNickName(String nickName);
}
