package com.npu.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 认证相关前端路由的 GET 回退（进入 Vue SPA）。
 *
 * <p><b>背景（既有缺陷）</b>：{@code AuthController} 对 {@code /login}、{@code /register}、
 * {@code /register/verify}、{@code /forgot-password}、{@code /reset-password} 仅有 <b>POST</b> 映射。
 * Spring MVC 的 {@code RequestMappingHandlerMapping}（order 0）在「路径能匹配、但请求方法不匹配」
 * 时会抛出 {@code HttpRequestMethodNotSupportedException}，因此<b>不会</b>继续交给
 * {@code WebConfig#addViewControllers} 注册的、order 更低的视图控制器映射。
 * 结果是浏览器直接访问或刷新这些地址会拿到 <b>HTTP 500</b>，而不是进入 SPA —— 而登录页正是系统入口页。
 *
 * <p><b>修法</b>：用一个带 GET 映射的 {@code @Controller} 把这些路径转发到 index.html。
 * 注意必须是 {@code @Controller} 而非 {@code @RestController}：后者的 {@code @ResponseBody}
 * 语义会把返回的字符串当作响应体写出，而不是当作视图名解析。
 *
 * <p>与既有 POST 接口不冲突：请求方法不同，属于同一 HandlerMapping 下的两个独立映射。
 * 本清单与 {@code SecurityConfig} 中 permitAll 的前端路由保持一致。
 */
@Controller
public class AuthSpaRouteController {

    @GetMapping({"/login", "/register", "/register/verify", "/forgot-password", "/reset-password"})
    public String authSpaRoutes() {
        return "forward:/index.html";
    }
}
