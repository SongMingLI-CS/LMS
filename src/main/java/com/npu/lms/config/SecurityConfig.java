package com.npu.lms.config;

import com.npu.lms.security.JwtAuthenticationFilter;
import com.npu.lms.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 2. 【新增】启用方法级安全注解
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return builder.build();
    }

    // (CORS 配置保持不变)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://localhost:63342",
                "null",
                "https://*.ngrok.io",
                "http://*.ngrok.io"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // P0: 安全响应头（CSP / HSTS / Referrer-Policy / Permissions-Policy 等）
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true))
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data: blob:; " +
                                "font-src 'self' data:; " +
                                "connect-src 'self'; " +
                                "object-src 'none'; " +
                                "frame-ancestors 'none'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'"))
                        .permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=()"))
                )

                .authorizeHttpRequests(authz -> authz


                        // --- 公开路径 (不变) ---
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**", "/analysis", "/books", "/records", "/audit", "/borrow", "/profile", "/users", "/login", "/register").permitAll()
                        .requestMatchers("/register/verify", "/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/search").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- (***关键修改***) ---

                        // --- 管理员权限 (ADMIN / SUPERADMIN) ---
                        // --- (新增!) 分析导出 (仅限管理员) ---
                        .requestMatchers("/api/analysis/export/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/books").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/records/return").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/records/process-reservation/**").hasAnyRole("ADMIN", "SUPERADMIN")

                        // (已存在的) 允许 ADMIN 读取用户列表 (GET /api/users)
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("ADMIN", "SUPERADMIN")

                        // (新增!) 允许 ADMIN 读取单个用户 (GET /api/users/{id})
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "SUPERADMIN")

                        // --- 超级管理员权限 (SUPERADMIN only) ---
                        // ... (后续配置不变)
                        .requestMatchers(HttpMethod.POST, "/api/users").hasAnyRole("SUPERADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyRole("SUPERADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAnyRole("SUPERADMIN")
                        // --- (新增!) 审计日志 (仅限超管) ---
                        .requestMatchers("/api/audit-logs/**").hasRole("SUPERADMIN")

                        // --- (新增!) 个人资料路径 (所有登录用户) ---
                        .requestMatchers("/api/profile/**").authenticated() // 确保所有登录用户都能访问自己的资料

                        // --- 其他 (不变) ---
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}