package com.example.tradewise.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 配置拦截器等
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 暂时注释掉登录拦截器，避免影响现有功能
        // 如需启用认证，请取消注释以下代码
        
        /*
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/**") // 拦截所有API请求
                .excludePathPatterns(
                    "/api/auth/login",      // 登录接口不拦截
                    "/api/auth/logout",     // 登出接口不拦截
                    "/api/orders/health",   // 健康检查不拦截
                    "/api/system-health/**" // 系统健康检查不拦截
                );
        */
    }
}
