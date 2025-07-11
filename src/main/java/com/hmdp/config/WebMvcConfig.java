package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置类
 *
 * @author Kraztoria rotroutine@163.com
 * @since 2025/7/11 15:14
 */
@Configuration
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    private final RefreshTokenInterceptor refreshTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(refreshTokenInterceptor);

        registry.addInterceptor(loginInterceptor).excludePathPatterns(
                "/user/code", "/user/login",
                "/blog/hot",
                "/shop/**", "/shop-type/**", "/upload/**", "/voucher/**"
        );

        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
