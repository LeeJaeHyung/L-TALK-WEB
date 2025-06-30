package com.ltalk.web.controller;

import com.ltalk.web.dto.LoginResult;
import com.ltalk.web.dto.response.LoginResponse;
import com.ltalk.web.dto.request.LoginRequest;
import com.ltalk.web.dto.request.SignUpRequest;
import com.ltalk.web.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MemberController {

    private MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }



    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@ModelAttribute LoginRequest request, HttpServletResponse response) {
        LoginResult result = memberService.login(request);// Redis에 저장됨
        // 쿠키 생성 및 설정
        Cookie cookie = new Cookie("access_token", result.getToken());
        cookie.setHttpOnly(true); // JavaScript로 접근 불가능 (XSS 방어)
        cookie.setPath("/");      // 모든 경로에서 전송
        cookie.setMaxAge(60 * 30); // 30분 유지

        response.addCookie(cookie); // 응답에 쿠키 추가

        // JSON 응답도 함께 반환 (선택)
        return ResponseEntity.ok(new LoginResponse("로그인 성공", result.getToken(), result.getLoginMember()));
    }


    @GetMapping("/sign-up")
    public String signUpPage(){
        return "sign-up";
    }


    @PostMapping("/sign-up")
    public String signUp(@Valid @ModelAttribute SignUpRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder message = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                message.append(error.getField())
                        .append(": ")
                        .append(error.getDefaultMessage())
                        .append("\n");
            }
            return "redirect:/sign-up?message=" + URLEncoder.encode(message.toString(), StandardCharsets.UTF_8);
        }

        memberService.signUp(request);
        return "redirect:/login?message=" + URLEncoder.encode("회원가입이 완료되었습니다!", StandardCharsets.UTF_8);
    }

    @GetMapping("/users/check-username")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> duplicateUsername(@RequestParam String username) {
        boolean exists = memberService.duplicateUsername(username);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("message", exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");
        System.out.println(exists);
        return ResponseEntity.ok(response);
    }



}
