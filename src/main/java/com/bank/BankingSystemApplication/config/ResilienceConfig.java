package com.bank.BankingSystemApplication.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerConfig bankingCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50f) // 50% de falhas para abrir o circuito
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Aguarda 30s antes de tentar novamente
                .slidingWindowSize(10) // Janela deslizante de 10 chamadas
                .minimumNumberOfCalls(5) // Mínimo de 5 chamadas para calcular taxa de falha
                .slowCallRateThreshold(50f) // 50% de chamadas lentas
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // Chamadas > 2s são consideradas lentas
                .permittedNumberOfCallsInHalfOpenState(3) // 3 chamadas permitidas no estado meio-aberto
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Bean
    public RetryConfig bankingRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // Máximo 3 tentativas
                .waitDuration(Duration.ofMillis(500)) // Aguarda 500ms entre tentativas
                .retryOnException(throwable -> 
                    throwable instanceof RuntimeException || 
                    throwable instanceof java.util.concurrent.TimeoutException)
                .build();
    }

    @Bean
    public TimeLimiterConfig bankingTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5)) // Timeout de 5 segundos
                .cancelRunningFuture(true)
                .build();
    }

    @Bean
    public RateLimiterConfig bankingRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1)) // Reset a cada minuto
                .limitForPeriod(100) // 100 requisições por minuto
                .timeoutDuration(Duration.ofSeconds(3)) // Timeout de 3s para adquirir permissão
                .build();
    }
}