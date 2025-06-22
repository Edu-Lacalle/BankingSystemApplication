package com.bank.BankingSystemApplication.adapter.in.web;

import com.bank.BankingSystemApplication.infrastructure.monitoring.PerformanceMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance Monitoring API", description = "API para monitoramento de performance, vazão, tempo de resposta e gargalos")
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceController {
    
    @Autowired
    private PerformanceMetricsService performanceMetricsService;
    
    @GetMapping("/stats")
    @Operation(summary = "Estatísticas Completas de Performance", 
               description = "Retorna todas as métricas de performance incluindo vazão, tempo de resposta e gargalos")
    public ResponseEntity<PerformanceMetricsService.PerformanceStats> getPerformanceStats() {
        PerformanceMetricsService.PerformanceStats stats = performanceMetricsService.getPerformanceStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/throughput")
    @Operation(summary = "Métricas de Vazão (Throughput)", 
               description = "Retorna TPS (Transactions Per Second) e QPS (Queries Per Second) atuais")
    public ResponseEntity<Map<String, Object>> getThroughputMetrics() {
        Map<String, Object> throughput = new HashMap<>();
        
        throughput.put("currentTPS", performanceMetricsService.getCurrentTPS());
        throughput.put("currentTransactionTPS", performanceMetricsService.getCurrentTransactionTPS());
        throughput.put("description", Map.of(
            "currentTPS", "Requisições por segundo",
            "currentTransactionTPS", "Transações por segundo"
        ));
        throughput.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(throughput);
    }
    
    @GetMapping("/response-time")
    @Operation(summary = "Métricas de Tempo de Resposta", 
               description = "Retorna estatísticas detalhadas de tempo de resposta incluindo percentis")
    public ResponseEntity<Map<String, Object>> getResponseTimeMetrics() {
        PerformanceMetricsService.PerformanceStats stats = performanceMetricsService.getPerformanceStats();
        
        Map<String, Object> responseTime = new HashMap<>();
        responseTime.put("averageResponseTime", stats.averageResponseTime);
        responseTime.put("p95ResponseTime", stats.p95ResponseTime);
        responseTime.put("p99ResponseTime", stats.p99ResponseTime);
        responseTime.put("unit", "milliseconds");
        responseTime.put("description", Map.of(
            "averageResponseTime", "Tempo médio de resposta",
            "p95ResponseTime", "95% das requisições são atendidas em até",
            "p99ResponseTime", "99% das requisições são atendidas em até"
        ));
        responseTime.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(responseTime);
    }
    
    @GetMapping("/bottlenecks")
    @Operation(summary = "Detecção de Gargalos", 
               description = "Identifica e retorna informações sobre gargalos de performance no sistema")
    public ResponseEntity<Map<String, Object>> getBottleneckAnalysis() {
        PerformanceMetricsService.PerformanceStats stats = performanceMetricsService.getPerformanceStats();
        PerformanceMetricsService.BottleneckInfo bottlenecks = stats.bottleneckInfo;
        PerformanceMetricsService.SlowestOperationInfo slowest = stats.slowestOperation;
        
        Map<String, Object> analysis = new HashMap<>();
        
        // Informações de gargalos por componente
        Map<String, Object> componentTimes = new HashMap<>();
        componentTimes.put("database", Map.of(
            "timeMs", bottlenecks.databaseTime,
            "status", bottlenecks.databaseTime > 1000 ? "slow" : "normal"
        ));
        componentTimes.put("kafka", Map.of(
            "timeMs", bottlenecks.kafkaTime,
            "status", bottlenecks.kafkaTime > 1000 ? "slow" : "normal"
        ));
        componentTimes.put("businessLogic", Map.of(
            "timeMs", bottlenecks.businessLogicTime,
            "status", bottlenecks.businessLogicTime > 1000 ? "slow" : "normal"
        ));
        
        analysis.put("componentPerformance", componentTimes);
        analysis.put("currentBottleneck", bottlenecks.currentBottleneck);
        analysis.put("slowestOperation", Map.of(
            "operation", slowest.operation,
            "timeMs", slowest.timeMs
        ));
        
        // Recomendações baseadas nos gargalos
        analysis.put("recommendations", generateRecommendations(bottlenecks));
        analysis.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(analysis);
    }
    
    @GetMapping("/health/performance")
    @Operation(summary = "Health Check de Performance", 
               description = "Verifica se a performance do sistema está dentro dos limites aceitáveis")
    public ResponseEntity<Map<String, Object>> getPerformanceHealth() {
        PerformanceMetricsService.PerformanceStats stats = performanceMetricsService.getPerformanceStats();
        
        Map<String, Object> health = new HashMap<>();
        
        // Critérios de saúde
        boolean tpsHealthy = stats.currentTPS < 1000; // Limite de 1000 TPS
        boolean responseTimeHealthy = stats.averageResponseTime < 500; // Limite de 500ms
        boolean bottleneckHealthy = stats.bottleneckInfo.currentBottleneck.equals("none");
        
        health.put("tps", Map.of(
            "status", tpsHealthy ? "healthy" : "warning",
            "value", stats.currentTPS,
            "limit", 1000
        ));
        
        health.put("responseTime", Map.of(
            "status", responseTimeHealthy ? "healthy" : "warning", 
            "value", stats.averageResponseTime,
            "limit", 500
        ));
        
        health.put("bottlenecks", Map.of(
            "status", bottleneckHealthy ? "healthy" : "warning",
            "value", stats.bottleneckInfo.currentBottleneck
        ));
        
        // Status geral
        boolean overallHealthy = tpsHealthy && responseTimeHealthy && bottleneckHealthy;
        health.put("overall", Map.of(
            "status", overallHealthy ? "healthy" : "degraded",
            "score", calculateHealthScore(tpsHealthy, responseTimeHealthy, bottleneckHealthy)
        ));
        
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard de Performance", 
               description = "Retorna um resumo completo para dashboard de monitoramento")
    public ResponseEntity<Map<String, Object>> getPerformanceDashboard() {
        PerformanceMetricsService.PerformanceStats stats = performanceMetricsService.getPerformanceStats();
        
        Map<String, Object> dashboard = new HashMap<>();
        
        // Métricas principais
        dashboard.put("mainMetrics", Map.of(
            "tps", stats.currentTPS,
            "transactionTps", stats.currentTransactionTPS,
            "avgResponseTime", stats.averageResponseTime,
            "p95ResponseTime", stats.p95ResponseTime
        ));
        
        // Status dos componentes
        dashboard.put("componentStatus", Map.of(
            "database", categorizePerformance(stats.bottleneckInfo.databaseTime),
            "kafka", categorizePerformance(stats.bottleneckInfo.kafkaTime),
            "businessLogic", categorizePerformance(stats.bottleneckInfo.businessLogicTime)
        ));
        
        // Alertas
        dashboard.put("alerts", generateAlerts(stats));
        
        // Operação mais lenta
        dashboard.put("slowestOperation", stats.slowestOperation);
        
        dashboard.put("timestamp", System.currentTimeMillis());
        dashboard.put("refreshInterval", 30); // segundos
        
        return ResponseEntity.ok(dashboard);
    }
    
    private Map<String, String> generateRecommendations(PerformanceMetricsService.BottleneckInfo bottlenecks) {
        Map<String, String> recommendations = new HashMap<>();
        
        if (bottlenecks.databaseTime > 1000) {
            recommendations.put("database", "Considere otimizar queries, adicionar índices ou aumentar pool de conexões");
        }
        
        if (bottlenecks.kafkaTime > 1000) {
            recommendations.put("kafka", "Verifique configurações do Kafka, latência de rede ou considere batch processing");
        }
        
        if (bottlenecks.businessLogicTime > 1000) {
            recommendations.put("businessLogic", "Analise algoritmos complexos ou considere processamento assíncrono");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.put("general", "Performance está dentro dos limites normais");
        }
        
        return recommendations;
    }
    
    private int calculateHealthScore(boolean tpsHealthy, boolean responseTimeHealthy, boolean bottleneckHealthy) {
        int score = 0;
        if (tpsHealthy) score += 33;
        if (responseTimeHealthy) score += 34;
        if (bottleneckHealthy) score += 33;
        return score;
    }
    
    private String categorizePerformance(long timeMs) {
        if (timeMs < 100) return "excellent";
        if (timeMs < 500) return "good";
        if (timeMs < 1000) return "fair";
        if (timeMs < 5000) return "poor";
        return "critical";
    }
    
    private Map<String, Object> generateAlerts(PerformanceMetricsService.PerformanceStats stats) {
        Map<String, Object> alerts = new HashMap<>();
        
        if (stats.currentTPS > 800) {
            alerts.put("highTPS", "TPS aproximando do limite (1000)");
        }
        
        if (stats.averageResponseTime > 400) {
            alerts.put("slowResponse", "Tempo de resposta elevado");
        }
        
        if (!stats.bottleneckInfo.currentBottleneck.equals("none")) {
            alerts.put("bottleneck", "Gargalo detectado em: " + stats.bottleneckInfo.currentBottleneck);
        }
        
        if (stats.slowestOperation.timeMs > 2000) {
            alerts.put("slowOperation", "Operação muito lenta detectada: " + stats.slowestOperation.operation);
        }
        
        return alerts;
    }
}