package com.bank.BankingSystemApplication.infrastructure.monitoring;

import com.bank.BankingSystemApplication.infrastructure.monitoring.PerformanceMetricsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Aspect
@Component
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);
    
    @Autowired
    private PerformanceMetricsService performanceMetricsService;
    
    @Around("execution(* com.bank.BankingSystemApplication.infrastructure.persistence.*.*(..))")
    public Object measureDatabasePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String operation = joinPoint.getSignature().toShortString();
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            performanceMetricsService.recordDatabaseResponseTime(duration);
            
            if (performanceMetricsService.isSlowOperation(duration)) {
                performanceMetricsService.recordBottleneck("database", operation, duration);
                logger.warn("Slow database operation: {} took {}ms", operation, duration.toMillis());
            }
            
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            performanceMetricsService.recordBottleneck("database", operation + "_error", duration);
            throw e;
        }
    }
    
    @Around("execution(* com.bank.BankingSystemApplication.application.service.kafka.*.*(..))")
    public Object measureKafkaPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String operation = joinPoint.getSignature().toShortString();
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            performanceMetricsService.recordKafkaResponseTime(duration);
            
            if (performanceMetricsService.isSlowOperation(duration)) {
                performanceMetricsService.recordBottleneck("kafka", operation, duration);
                logger.warn("Slow Kafka operation: {} took {}ms", operation, duration.toMillis());
            }
            
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            performanceMetricsService.recordBottleneck("kafka", operation + "_error", duration);
            throw e;
        }
    }
    
    @Around("execution(* com.bank.BankingSystemApplication.domain.service.*.*(..))")
    public Object measureBusinessLogicPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String operation = joinPoint.getSignature().toShortString();
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            performanceMetricsService.recordBusinessLogicTime(duration);
            
            if (performanceMetricsService.isSlowOperation(duration)) {
                performanceMetricsService.recordBottleneck("business", operation, duration);
                logger.warn("Slow business logic operation: {} took {}ms", operation, duration.toMillis());
            }
            
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            performanceMetricsService.recordBottleneck("business", operation + "_error", duration);
            throw e;
        }
    }
    
    @Around("execution(* com.bank.BankingSystemApplication.adapter.out.messaging.*.*(..))")
    public Object measureMessagingPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String operation = joinPoint.getSignature().toShortString();
        
        try {
            Object result = joinPoint.proceed();
            Duration duration = Duration.between(start, Instant.now());
            
            if (operation.contains("kafka") || operation.contains("Kafka")) {
                performanceMetricsService.recordKafkaResponseTime(duration);
            }
            
            if (performanceMetricsService.isSlowOperation(duration)) {
                performanceMetricsService.recordBottleneck("messaging", operation, duration);
                logger.warn("Slow messaging operation: {} took {}ms", operation, duration.toMillis());
            }
            
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            performanceMetricsService.recordBottleneck("messaging", operation + "_error", duration);
            throw e;
        }
    }
}