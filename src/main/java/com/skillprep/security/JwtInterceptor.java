package com.skillprep.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true; // browser pre-check, no token on it

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String userId = jwtUtil.getUserId(header.substring(7));
            if (userId != null) {
                request.setAttribute("userId", userId);
                return true;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Please sign in again.\"}");
        return false;
    }
}
