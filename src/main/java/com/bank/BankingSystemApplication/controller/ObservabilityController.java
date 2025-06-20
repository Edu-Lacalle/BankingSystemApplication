package com.bank.BankingSystemApplication.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para observabilidade e métricas do sistema
 */
@RestController
@RequestMapping("/api/observability")
@Tag(name = "Observability API", description = "API para monitoramento e observabilidade do sistema")
public class ObservabilityController {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    
    @Autowired
    private RetryRegistry retryRegistry;
    
    /**
     * Status dos Circuit Breakers
     */
    @GetMapping("/circuit-breakers")
    @Operation(summary = "Status dos Circuit Breakers", description = "Retorna o status atual de todos os circuit breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakersStatus() {
        Map<String, Object> status = new HashMap<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            Map<String, Object> cbInfo = new HashMap<>();
            cbInfo.put("state", circuitBreaker.getState().toString());
            cbInfo.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
            cbInfo.put("numberOfCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
            cbInfo.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
            cbInfo.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            
            status.put(circuitBreaker.getName(), cbInfo);
        });
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Status dos Rate Limiters
     */
    @GetMapping("/rate-limiters")
    @Operation(summary = "Status dos Rate Limiters", description = "Retorna o status atual de todos os rate limiters")
    public ResponseEntity<Map<String, Object>> getRateLimitersStatus() {
        Map<String, Object> status = new HashMap<>();
        
        rateLimiterRegistry.getAllRateLimiters().forEach(rateLimiter -> {
            Map<String, Object> rlInfo = new HashMap<>();
            rlInfo.put("availablePermissions", rateLimiter.getMetrics().getAvailablePermissions());
            rlInfo.put("numberOfWaitingThreads", rateLimiter.getMetrics().getNumberOfWaitingThreads());
            
            status.put(rateLimiter.getName(), rlInfo);
        });
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Status dos Retries
     */
    @GetMapping("/retries")
    @Operation(summary = "Status dos Retries", description = "Retorna o status atual de todos os retries")
    public ResponseEntity<Map<String, Object>> getRetriesStatus() {
        Map<String, Object> status = new HashMap<>();
        
        retryRegistry.getAllRetries().forEach(retry -> {
            Map<String, Object> retryInfo = new HashMap<>();
            retryInfo.put("numberOfSuccessfulCallsWithoutRetryAttempt", 
                         retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryInfo.put("numberOfSuccessfulCallsWithRetryAttempt", 
                         retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            retryInfo.put("numberOfFailedCallsWithoutRetryAttempt", 
                         retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            retryInfo.put("numberOfFailedCallsWithRetryAttempt", 
                         retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
            
            status.put(retry.getName(), retryInfo);
        });
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Health Check customizado para o sistema bancário
     */
    @GetMapping("/health/banking")
    @Operation(summary = "Health Check Bancário", description = "Verifica a saúde específica dos componentes bancários")
    public ResponseEntity<Map<String, Object>> getBankingHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Verificar Circuit Breakers
        boolean circuitBreakersHealthy = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .allMatch(cb -> !cb.getState().toString().equals("OPEN"));
        
        health.put("circuitBreakers", circuitBreakersHealthy ? "UP" : "DOWN");
        
        // Verificar Rate Limiters (se há permissões disponíveis)
        boolean rateLimitersHealthy = rateLimiterRegistry.getAllRateLimiters()
                .stream()
                .allMatch(rl -> rl.getMetrics().getAvailablePermissions() > 0);
        
        health.put("rateLimiters", rateLimitersHealthy ? "UP" : "DEGRADED");
        
        // Status geral
        boolean overallHealthy = circuitBreakersHealthy && rateLimitersHealthy;
        health.put("status", overallHealthy ? "UP" : "DEGRADED");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Métricas resumidas do sistema
     */
    @GetMapping("/metrics/summary")
    @Operation(summary = "Resumo de Métricas", description = "Retorna um resumo das principais métricas do sistema")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Métricas de Circuit Breakers
        Map<String, Object> cbMetrics = new HashMap<>();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            cbMetrics.put(cb.getName() + "_state", cb.getState().toString());
            cbMetrics.put(cb.getName() + "_failure_rate", cb.getMetrics().getFailureRate());
        });
        metrics.put("circuitBreakers", cbMetrics);
        
        // Métricas de Rate Limiters
        Map<String, Object> rlMetrics = new HashMap<>();
        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            rlMetrics.put(rl.getName() + "_available_permissions", rl.getMetrics().getAvailablePermissions());
            rlMetrics.put(rl.getName() + "_waiting_threads", rl.getMetrics().getNumberOfWaitingThreads());
        });
        metrics.put("rateLimiters", rlMetrics);
        
        // Métricas de Retry
        Map<String, Object> retryMetrics = new HashMap<>();
        retryRegistry.getAllRetries().forEach(retry -> {
            retryMetrics.put(retry.getName() + "_successful_without_retry", 
                           retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryMetrics.put(retry.getName() + "_successful_with_retry", 
                           retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
        });
        metrics.put("retries", retryMetrics);
        
        metrics.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(metrics);
    }
}