package com.bank.BankingSystemApplication.infrastructure.monitoring;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Service
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Métricas de Vazão (Throughput)
    private final Counter totalRequestsCounter;
    private final Counter totalResponsesCounter;
    private final Timer requestDurationTimer;
    
    // Métricas de Tempo de Resposta detalhadas
    private final DistributionSummary responseSizeDistribution;
    private final Timer.Builder responseTimeBuilder;
    
    // Métricas de Gargalos
    private final AtomicLong databaseConnectionTime = new AtomicLong(0);
    private final AtomicLong kafkaPublishTime = new AtomicLong(0);
    private final AtomicLong businessLogicTime = new AtomicLong(0);
    private final AtomicReference<String> slowestOperation = new AtomicReference<>("none");
    private final AtomicLong slowestOperationTime = new AtomicLong(0);
    
    // Contadores de TPS/QPS por período
    private final ConcurrentHashMap<String, LongAdder> throughputCounters = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastThroughputReset = new AtomicReference<>(Instant.now());
    
    // Limites para detecção de gargalos (em milissegundos)
    private static final long SLOW_OPERATION_THRESHOLD = 1000; // 1 segundo
    private static final long VERY_SLOW_OPERATION_THRESHOLD = 5000; // 5 segundos
    
    @Autowired
    public PerformanceMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Inicializar métricas de vazão
        this.totalRequestsCounter = Counter.builder("banking.requests.total")
                .description("Total de requisições recebidas")
                .register(meterRegistry);
                
        this.totalResponsesCounter = Counter.builder("banking.responses.total")
                .description("Total de respostas enviadas")
                .register(meterRegistry);
                
        this.requestDurationTimer = Timer.builder("banking.request.duration")
                .description("Duração completa da requisição")
                .register(meterRegistry);
        
        // Métricas de tempo de resposta
        this.responseSizeDistribution = DistributionSummary.builder("banking.response.size")
                .description("Tamanho da resposta em bytes")
                .register(meterRegistry);
                
        this.responseTimeBuilder = Timer.builder("banking.response.time")
                .description("Tempo de resposta por endpoint");
        
        // Métricas de gargalos
        Gauge.builder("banking.bottleneck.database.time", databaseConnectionTime, AtomicLong::get)
                .description("Tempo médio de conexão com database (ms)")
                .register(meterRegistry);
                
        Gauge.builder("banking.bottleneck.kafka.time", kafkaPublishTime, AtomicLong::get)
                .description("Tempo médio de publicação no Kafka (ms)")
                .register(meterRegistry);
                
        Gauge.builder("banking.bottleneck.business.time", businessLogicTime, AtomicLong::get)
                .description("Tempo médio de processamento da lógica de negócio (ms)")
                .register(meterRegistry);
                
        Gauge.builder("banking.bottleneck.slowest.time", slowestOperationTime, AtomicLong::get)
                .description("Tempo da operação mais lenta (ms)")
                .register(meterRegistry);
        
        // Métricas de TPS/QPS
        Gauge.builder("banking.throughput.requests.per.second", this, PerformanceMetricsService::getCurrentTPS)
                .description("Requisições por segundo atual")
                .register(meterRegistry);
                
        Gauge.builder("banking.throughput.transactions.per.second", this, PerformanceMetricsService::getCurrentTransactionTPS)
                .description("Transações por segundo atual")
                .register(meterRegistry);
    }
    
    // Métodos para Vazão (Throughput)
    public void recordRequest(String endpoint) {
        totalRequestsCounter.increment();
        incrementThroughputCounter("requests");
        incrementThroughputCounter("endpoint." + endpoint);
    }
    
    public void recordResponse(String endpoint, long responseSizeBytes) {
        totalResponsesCounter.increment();
        responseSizeDistribution.record(responseSizeBytes);
        incrementThroughputCounter("responses");
    }
    
    public void recordRequestDuration(String endpoint, Duration duration) {
        requestDurationTimer.record(duration);
        responseTimeBuilder.tag("endpoint", endpoint).register(meterRegistry).record(duration);
        
        // Verificar se é uma operação lenta
        long durationMs = duration.toMillis();
        if (durationMs > slowestOperationTime.get()) {
            slowestOperationTime.set(durationMs);
            slowestOperation.set(endpoint);
        }
    }
    
    // Métodos para Tempo de Resposta detalhado
    public Timer.Sample startResponseTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordResponseTime(Timer.Sample sample, String operation, String status) {
        Timer timer = Timer.builder("banking.operation.time")
                .tag("operation", operation)
                .tag("status", status)
                .register(meterRegistry);
        sample.stop(timer);
    }
    
    public void recordDatabaseResponseTime(Duration duration) {
        databaseConnectionTime.set(duration.toMillis());
        Timer.builder("banking.database.response.time")
                .description("Tempo de resposta do database")
                .register(meterRegistry)
                .record(duration);
    }
    
    public void recordKafkaResponseTime(Duration duration) {
        kafkaPublishTime.set(duration.toMillis());
        Timer.builder("banking.kafka.response.time")
                .description("Tempo de resposta do Kafka")
                .register(meterRegistry)
                .record(duration);
    }
    
    public void recordBusinessLogicTime(Duration duration) {
        businessLogicTime.set(duration.toMillis());
        Timer.builder("banking.business.logic.time")
                .description("Tempo de processamento da lógica de negócio")
                .register(meterRegistry)
                .record(duration);
    }
    
    // Métodos para Detecção de Gargalos
    public boolean isSlowOperation(Duration duration) {
        return duration.toMillis() > SLOW_OPERATION_THRESHOLD;
    }
    
    public boolean isVerySlowOperation(Duration duration) {
        return duration.toMillis() > VERY_SLOW_OPERATION_THRESHOLD;
    }
    
    public void recordBottleneck(String component, String operation, Duration duration) {
        long durationMs = duration.toMillis();
        
        // Registrar gargalo específico
        Counter.builder("banking.bottleneck.detected")
                .tag("component", component)
                .tag("operation", operation)
                .tag("severity", durationMs > VERY_SLOW_OPERATION_THRESHOLD ? "critical" : "warning")
                .register(meterRegistry)
                .increment();
        
        // Atualizar tempo do componente
        switch (component.toLowerCase()) {
            case "database":
                databaseConnectionTime.set(Math.max(databaseConnectionTime.get(), durationMs));
                break;
            case "kafka":
                kafkaPublishTime.set(Math.max(kafkaPublishTime.get(), durationMs));
                break;
            case "business":
                businessLogicTime.set(Math.max(businessLogicTime.get(), durationMs));
                break;
        }
    }
    
    // Métodos auxiliares para TPS/QPS
    private void incrementThroughputCounter(String key) {
        throughputCounters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }
    
    public double getCurrentTPS() {
        return calculateThroughput("requests");
    }
    
    public double getCurrentTransactionTPS() {
        return calculateThroughput("responses");
    }
    
    private double calculateThroughput(String key) {
        LongAdder counter = throughputCounters.get(key);
        if (counter == null) return 0.0;
        
        Instant now = Instant.now();
        Instant lastReset = lastThroughputReset.get();
        
        // Resetar contadores a cada minuto para TPS atual
        if (Duration.between(lastReset, now).toSeconds() >= 60) {
            if (lastThroughputReset.compareAndSet(lastReset, now)) {
                throughputCounters.values().forEach(LongAdder::reset);
                return 0.0;
            }
        }
        
        long elapsedSeconds = Duration.between(lastReset, now).toSeconds();
        if (elapsedSeconds == 0) return 0.0;
        
        return (double) counter.sum() / elapsedSeconds;
    }
    
    // Métodos para obter estatísticas
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
            getCurrentTPS(),
            getCurrentTransactionTPS(),
            getAverageResponseTime(),
            getP95ResponseTime(),
            getP99ResponseTime(),
            getBottleneckInfo(),
            getSlowestOperation()
        );
    }
    
    private double getAverageResponseTime() {
        Timer timer = meterRegistry.find("banking.request.duration").timer();
        return timer != null ? timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
    }
    
    private double getP95ResponseTime() {
        Timer timer = meterRegistry.find("banking.request.duration").timer();
        return timer != null ? timer.percentile(0.95, java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
    }
    
    private double getP99ResponseTime() {
        Timer timer = meterRegistry.find("banking.request.duration").timer();
        return timer != null ? timer.percentile(0.99, java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
    }
    
    private BottleneckInfo getBottleneckInfo() {
        return new BottleneckInfo(
            databaseConnectionTime.get(),
            kafkaPublishTime.get(),
            businessLogicTime.get(),
            identifyCurrentBottleneck()
        );
    }
    
    private String identifyCurrentBottleneck() {
        long dbTime = databaseConnectionTime.get();
        long kafkaTime = kafkaPublishTime.get();
        long businessTime = businessLogicTime.get();
        
        if (dbTime > kafkaTime && dbTime > businessTime && dbTime > SLOW_OPERATION_THRESHOLD) {
            return "database";
        } else if (kafkaTime > businessTime && kafkaTime > SLOW_OPERATION_THRESHOLD) {
            return "kafka";
        } else if (businessTime > SLOW_OPERATION_THRESHOLD) {
            return "business-logic";
        }
        
        return "none";
    }
    
    private SlowestOperationInfo getSlowestOperation() {
        return new SlowestOperationInfo(
            slowestOperation.get(),
            slowestOperationTime.get()
        );
    }
    
    // Classes de dados para estatísticas
    public static class PerformanceStats {
        public final double currentTPS;
        public final double currentTransactionTPS;
        public final double averageResponseTime;
        public final double p95ResponseTime;
        public final double p99ResponseTime;
        public final BottleneckInfo bottleneckInfo;
        public final SlowestOperationInfo slowestOperation;
        
        public PerformanceStats(double currentTPS, double currentTransactionTPS, 
                              double averageResponseTime, double p95ResponseTime, 
                              double p99ResponseTime, BottleneckInfo bottleneckInfo,
                              SlowestOperationInfo slowestOperation) {
            this.currentTPS = currentTPS;
            this.currentTransactionTPS = currentTransactionTPS;
            this.averageResponseTime = averageResponseTime;
            this.p95ResponseTime = p95ResponseTime;
            this.p99ResponseTime = p99ResponseTime;
            this.bottleneckInfo = bottleneckInfo;
            this.slowestOperation = slowestOperation;
        }
    }
    
    public static class BottleneckInfo {
        public final long databaseTime;
        public final long kafkaTime;
        public final long businessLogicTime;
        public final String currentBottleneck;
        
        public BottleneckInfo(long databaseTime, long kafkaTime, 
                            long businessLogicTime, String currentBottleneck) {
            this.databaseTime = databaseTime;
            this.kafkaTime = kafkaTime;
            this.businessLogicTime = businessLogicTime;
            this.currentBottleneck = currentBottleneck;
        }
    }
    
    public static class SlowestOperationInfo {
        public final String operation;
        public final long timeMs;
        
        public SlowestOperationInfo(String operation, long timeMs) {
            this.operation = operation;
            this.timeMs = timeMs;
        }
    }
}