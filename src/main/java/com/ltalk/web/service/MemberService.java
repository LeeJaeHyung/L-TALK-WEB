package com.ltalk.web.service;

import com.ltalk.web.entity.Member;
import com.ltalk.web.repository.MemberRepository;
import com.ltalk.web.request.SignUpRequest;
import org.springframework.stereotype.Service;

import static com.ltalk.web.util.PasswordEncoder.encode;
import static com.ltalk.web.util.PasswordEncoder.generateSalt;

@Service
public class MemberService {

    private MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
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
        }
        // salt 와 hashedPassword 저장
    }
}
