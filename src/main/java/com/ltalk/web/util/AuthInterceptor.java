package com.ltalk.web.util;

import com.ltalk.web.dto.LoginMemberDto;
import com.ltalk.web.service.RedisLoginTokenService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final RedisLoginTokenService redisLoginTokenService;

    public AuthInterceptor(RedisLoginTokenService redisLoginTokenService) {
        this.redisLoginTokenService = redisLoginTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }

        System.out.println("인터셉터 동작 확인: " + request.getRequestURI());
        LoginMemberDto loginMemberDto = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    loginMemberDto = redisLoginTokenService.get(cookie.getValue());
                    break;
                }
            }
        }

        if (loginMemberDto != null) {
            // 인증된 사용자면 request에 담고 다음으로
            request.setAttribute("member", loginMemberDto);
            return true;
        }

        // 인증되지 않은 사용자면 로그인 페이지로 리다이렉트
        String loginPage = request.getContextPath() + "/login";
        response.sendRedirect(loginPage);
        return false;
    }
}

