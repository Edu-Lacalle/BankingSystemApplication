package com.bank.BankingSystemApplication.infrastructure.monitoring;

import com.bank.BankingSystemApplication.infrastructure.monitoring.PerformanceMetricsService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String TIMER_SAMPLE_ATTRIBUTE = "timerSample";
    
    @Autowired
    private PerformanceMetricsService performanceMetricsService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Instant startTime = Instant.now();
        Timer.Sample timerSample = performanceMetricsService.startResponseTimer();
        
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        request.setAttribute(TIMER_SAMPLE_ATTRIBUTE, timerSample);
        
        // Registrar início da requisição
        String endpoint = getEndpointFromRequest(request);
        performanceMetricsService.recordRequest(endpoint);
        
        // Adicionar informações ao MDC para logs estruturados
        MDC.put("endpoint", endpoint);
        MDC.put("method", request.getMethod());
        MDC.put("startTime", startTime.toString());
        
        logger.debug("Request started: {} {}", request.getMethod(), endpoint);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        
        Instant startTime = (Instant) request.getAttribute(START_TIME_ATTRIBUTE);
        Timer.Sample timerSample = (Timer.Sample) request.getAttribute(TIMER_SAMPLE_ATTRIBUTE);
        
        if (startTime != null && timerSample != null) {
            Instant endTime = Instant.now();
            Duration totalDuration = Duration.between(startTime, endTime);
            
            String endpoint = getEndpointFromRequest(request);
            String status = ex != null ? "error" : "success";
            
            // Registrar métricas de performance
            performanceMetricsService.recordRequestDuration(endpoint, totalDuration);
            performanceMetricsService.recordResponseTime(timerSample, endpoint, status);
            
            // Estimar tamanho da resposta (aproximação)
            long responseSize = estimateResponseSize(response);
            performanceMetricsService.recordResponse(endpoint, responseSize);
            
            // Verificar se é uma operação lenta
            if (performanceMetricsService.isSlowOperation(totalDuration)) {
                logger.warn("Slow operation detected: {} {} took {}ms", 
                           request.getMethod(), endpoint, totalDuration.toMillis());
                
                if (performanceMetricsService.isVerySlowOperation(totalDuration)) {
                    logger.error("Very slow operation detected: {} {} took {}ms", 
                               request.getMethod(), endpoint, totalDuration.toMillis());
                }
            }
            
            // Adicionar informações finais ao MDC
            MDC.put("duration", String.valueOf(totalDuration.toMillis()));
            MDC.put("status", status);
            MDC.put("responseSize", String.valueOf(responseSize));
            
            logger.info("Request completed: {} {} - {}ms - {} - {}bytes", 
                       request.getMethod(), endpoint, totalDuration.toMillis(), 
                       response.getStatus(), responseSize);
        }
        
        // Limpar MDC
        MDC.clear();
    }
    
    private String getEndpointFromRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        if (uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        
        // Normalizar IDs para agrupamento de métricas
        return normalizeEndpoint(uri);
    }
    
    private String normalizeEndpoint(String uri) {
        // Substituir IDs numéricos por placeholder para agrupamento
        return uri.replaceAll("/\\d+", "/{id}")
                 .replaceAll("/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "/{uuid}");
    }
    
    private long estimateResponseSize(HttpServletResponse response) {
        // Estimativa básica baseada no Content-Length header
        String contentLength = response.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength);
            } catch (NumberFormatException e) {
                // Ignorar erro e usar estimativa padrão
            }
        }
        
        // Estimativa baseada no status code
        int status = response.getStatus();
        if (status >= 400) {
            return 500; // Estimativa para respostas de erro
        } else if (status == 204) {
            return 0; // No Content
        } else {
            return 1000; // Estimativa padrão para respostas de sucesso
        }
    }
}