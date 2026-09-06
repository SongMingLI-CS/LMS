package com.npu.lms.security;

import com.npu.lms.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String jti = tokenProvider.getJtiFromToken(jwt);
                String username = tokenProvider.getUsernameFromToken(jwt);

                // P0: 拒绝已登出撤销的令牌
                if (!tokenService.isAccessTokenRevoked(jti)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // P0: 校验令牌版本，改密后的旧令牌一律失效
                    long tokenVersion = tokenProvider.getTokenVersionFromToken(jwt);
                    boolean versionOk = !(userDetails instanceof User)
                            || ((User) userDetails).getTokenVersion() == tokenVersion;

                    if (versionOk) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        // 放入 Spring Security 上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("无法设置用户认证", ex);
        }

        filterChain.doFilter(request, response);
    }

    // 从请求头中获取 "Authorization: Bearer <token>"
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}