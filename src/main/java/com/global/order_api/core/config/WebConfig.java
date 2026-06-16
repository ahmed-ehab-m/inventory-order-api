package com.global.order_api.core.config;

import com.global.order_api.core.rate_limiting.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/// web mvc configurer => interface allows us to configure web settings without editing default
/// use it to override addInterceptor()
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /// add our interceptor and specify paths
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}