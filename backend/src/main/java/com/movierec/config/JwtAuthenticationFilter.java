package com.movierec.config;

import com.movierec.entity.User;
import com.movierec.mapper.UserAuthorizationMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.service.AuthSessionService;
import com.movierec.service.SecurityAuditService;
import com.movierec.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UserAuthorizationMapper authorizationMapper;
    private final AuthSessionService sessionService;
    private final SecurityAuditService auditService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper,
                                   UserAuthorizationMapper authorizationMapper,
                                   AuthSessionService sessionService, SecurityAuditService auditService) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.authorizationMapper = authorizationMapper;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            String sessionId = jwtUtil.getSessionIdFromToken(token);
            User user = userMapper.selectById(userId);
            if (sessionService.isSessionActive(sessionId)
                    && user != null && (user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus()))) {
                String role = "ADMIN".equalsIgnoreCase(user.getRole()) ? "ROLE_ADMIN" : "ROLE_USER";
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority(role));
                List<String> permissions = authorizationMapper.findPermissionCodes(user.getId());
                if (permissions != null) permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                authentication.setDetails(sessionId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                auditService.record("ACCESS_TOKEN", "DENIED", userId, request.getRequestURI(),
                        "reason=inactive_session_or_user");
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : null;
    }
}
