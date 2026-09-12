package com.npu.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SPA 路由与视图配置。
 *
 * <p>CORS 统一由 {@link SecurityConfig#corsConfigurationSource()} 提供（Security 过滤器链已注册
 * {@code /**}），此处不再重复配置，避免两处规则不一致。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * SPA 前端路由回退：/books、/analysis 等客户端路由直链/刷新时，
     * 将请求转发到 index.html，由 Vue Router 接管渲染（配合 SecurityConfig 的放行配置）。
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        String[] spaRoutes = { "/analysis", "/books", "/records", "/audit", "/borrow", "/profile", "/users" };
        for (String route : spaRoutes) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }
}
