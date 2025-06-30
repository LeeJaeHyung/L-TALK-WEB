package com.ltalk.web.service;

import com.ltalk.web.dto.LoginMemberDto;
import com.ltalk.web.dto.LoginResult;
import com.ltalk.web.entity.Member;
import com.ltalk.web.repository.MemberRepository;
import com.ltalk.web.dto.request.LoginRequest;
import com.ltalk.web.dto.request.SignUpRequest;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ltalk.web.util.PasswordEncoder.*;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final RedisLoginTokenService redisLoginTokenService;

    public MemberService(MemberRepository memberRepository, RedisLoginTokenService redisLoginTokenService) {
        this.memberRepository = memberRepository;
        this.redisLoginTokenService = redisLoginTokenService;
    }

    public void signUp(SignUpRequest request) {
        if(!memberRepository.existsByUserNameAndNickNameAndEmail(request.getUserName(), request.getNickName(), request.getEmail())){
            String salt = generateSalt();
            String hashedPassword = encode(request.getPassword(), salt);
            request.setPassword(hashedPassword);
            Member member = new Member(request, salt);
            memberRepository.save(member);
        }else{
            System.out.println("이미 맴버 존재");
            throw new IllegalArgumentException("아이디와 닉네임 이메일을 다시한번 중복 확인해주세요.");
        }
        // salt 와 hashedPassword 저장
    }

    public LoginResult login(LoginRequest request) {
        Member member = memberRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        String hashedPassword = member.getPassword();
        String salt = member.getSalt();

        if (comparePassword(request.getPassword(), salt, hashedPassword)) {
            //토큰 생성
            String token = UUID.randomUUID().toString();

            // Redis에 로그인 정보 저장
            LoginMemberDto dto = new LoginMemberDto(
                    member.getId(),
                    member.getUserName(),
                    member.getNickName(),
                    member.getUserRole().name()
            );

            redisLoginTokenService.save(token, dto);// Duration 설정 포함됨
            return new LoginResult(token, dto);
        } else {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }
    }


    public boolean duplicateUsername(String username) {
        return memberRepository.existsByUserName(username);
    }
}
