package com.patches.plm.config;

import com.patches.plm.web.PermissionCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionCheckInterceptor permissionCheckInterceptor;

    public WebMvcConfig(PermissionCheckInterceptor permissionCheckInterceptor) {
        this.permissionCheckInterceptor = permissionCheckInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionCheckInterceptor).addPathPatterns("/api/**");
    }
}
