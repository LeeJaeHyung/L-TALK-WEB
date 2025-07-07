package com.ltalk.web.member.service;


import com.ltalk.web.global.dto.LoginMemberDto;
import com.ltalk.web.global.service.RedisLoginTokenService;
import com.ltalk.web.member.domain.Member;
import com.ltalk.web.member.dto.request.LoginRequest;
import com.ltalk.web.member.dto.request.MemberPatchRequest;
import com.ltalk.web.member.dto.request.SignUpRequest;
import com.ltalk.web.member.dto.response.LoginResult;
import com.ltalk.web.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.ltalk.web.global.config.PasswordEncoder.*;


@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final RedisLoginTokenService redisLoginTokenService;

    public MemberService(MemberRepository memberRepository,RedisLoginTokenService redisLoginTokenService) {
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
                    member.getEmail(),
                    member.getPhoneNumber(),
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

    public boolean duplicateEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public boolean duplicateNickName(String nickName) {
        return memberRepository.existsByNickName(nickName);
    }

    @Transactional
    public Member updateMyInfo(Member member, MemberPatchRequest memberPatchRequest) {
        Member targetMember = memberRepository.findById(member.getId()).orElseThrow(()-> new IllegalArgumentException("일치하는 사용자가 없습니다."));
        if(memberPatchRequest.getPassword()!=null){//비밀번호 해쉬 처리 -> 이부분 어디에 위치하는게 좋은지 모르겠음 update()에 위치해야 할지 어느곳이 좋은지 모르겠음
            memberPatchRequest.setPassword(encode(memberPatchRequest.getPassword(), targetMember.getSalt()));
        }
        targetMember.update(memberPatchRequest.getNickname(),memberPatchRequest.getPassword(), memberPatchRequest.getEmail(), memberPatchRequest.getPhoneNumber());
        return targetMember;
    }

    @Transactional
    public void deleteMyInfo(Member member, HttpServletRequest request, HttpServletResponse response) {
        Member target = memberRepository.findById(member.getId()).orElseThrow(()->new IllegalArgumentException("존재하지 않는 멤버입니다."));
        target.softDelete();
        Cookie[] cookies = request.getCookies();
        for(Cookie token : cookies){
            if(token.getName().equals("access_token")){
                token.setMaxAge(0);
                token.setPath("/"); // 중요! 생성 시 path와 같아야 삭제됨
                response.addCookie(token);
                redisLoginTokenService.remove(token.getValue(), member.getId());
            }
        }
    }
}
