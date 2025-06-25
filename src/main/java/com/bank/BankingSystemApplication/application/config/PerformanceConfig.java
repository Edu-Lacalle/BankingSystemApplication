package com.bank.BankingSystemApplication.application.config;

import com.bank.BankingSystemApplication.infrastructure.monitoring.PerformanceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAspectJAutoProxy
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceConfig implements WebMvcConfigurer {
    
    @Autowired
    private PerformanceInterceptor performanceInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/performance/**",     // Evitar recursão nos endpoints de performance
                    "/actuator/**",           // Evitar overhead nos health checks
                    "/swagger-ui/**",         // Evitar noise na documentação
                    "/api-docs/**"            // Evitar noise na documentação
                );
    }
}