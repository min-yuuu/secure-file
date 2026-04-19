package com.securefile.config;

import com.securefile.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置 - 注册拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")  // 拦截所有API
                .excludePathPatterns(
                        "/api/v1/auth/register",     // 排除注册
                        "/api/v1/auth/login",        // 排除登录
                        "/doc.html",                 // 排除Knife4j文档页面
                        "/doc.html/**",              // 排除Knife4j文档子页面
                        "/swagger-resources/**",     // 排除Swagger资源
                        "/v2/api-docs",              // 排除Swagger API文档
                        "/v2/api-docs-ext",          // 排除Knife4j扩展API文档
                        "/webjars/**",               // 排除webjars资源
                        "/swagger-ui.html",          // 排除Swagger UI
                        "/swagger-ui/**",            // 排除Swagger UI资源
                        "/favicon.ico"               // 排除图标
                );
    }
}
