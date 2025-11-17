package com.npu.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 这允许来自 file:// (null), localhost:xxxx 等的前端访问
        registry.addMapping("/**") // 允许所有路径
                .allowedOrigins("http://localhost:8080", "http://127.0.0.1:8080", "http://localhost:63342", "null") // 允许的前端地址
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}