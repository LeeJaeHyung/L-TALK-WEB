package com.ltalk.web.controller;

import com.ltalk.web.request.LoginRequest;
import com.ltalk.web.request.SignUpRequest;
import com.ltalk.web.service.MemberService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class MemberController {

    private MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }



    @ResponseBody
    @PostMapping("/login")
    public void login(@ModelAttribute LoginRequest request){
        System.out.println(request.getUserName()+" "+request.getPassword());
        System.out.println("로그인 요청");
    }

    @GetMapping("/sign-up")
    public String signUpPage(){
        return "sign-up";
    }

    @PostMapping("/sign-up")
    public String signUp(@ModelAttribute SignUpRequest request){
        System.out.println(request);
        memberService.signUp(request);
        return "/home";
    }


}
