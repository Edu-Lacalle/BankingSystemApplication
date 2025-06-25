package com.bank.BankingSystemApplication.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DatadogTracingService {

    private static final Logger logger = LoggerFactory.getLogger(DatadogTracingService.class);

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${datadog.service.name:banking-system}")
    private String serviceName;

    @Value("${datadog.environment:local}")
    private String environment;

    private final Map<String, Timer.Sample> activeTimers = new ConcurrentHashMap<>();
    private final Counter accountCreationCounter;
    private final Counter transactionCounter;
    private final Counter errorCounter;

    public DatadogTracingService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.accountCreationCounter = Counter.builder("banking.account.creation")
                .description("Number of account creation operations")
                .register(meterRegistry);
        this.transactionCounter = Counter.builder("banking.transaction")
                .description("Number of banking transactions")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("banking.error")
                .description("Number of banking operation errors")
                .register(meterRegistry);
    }

    public BankingSpan startAccountCreation(String cpf) {
        String operationId = "account_creation_" + System.nanoTime();
        String operationName = "banking.account.create";
        
        // Enhanced MDC context for log correlation
        MDC.put("operation.id", operationId);
        MDC.put("operation.type", "account_creation");
        MDC.put("business.domain", "banking");
        MDC.put("customer.cpf.masked", maskCpf(cpf));
        MDC.put("service.name", serviceName);
        MDC.put("environment", environment);
        
        // Start Micrometer timer
        Timer.Sample timerSample = Timer.start(meterRegistry);
        activeTimers.put(operationId, timerSample);
        
        logger.info("Starting account creation for customer: {}", maskCpf(cpf));
        accountCreationCounter.increment();
        
        return new BankingSpan(operationId, operationName, timerSample);
    }

    public BankingSpan startTransaction(String accountId, String transactionType, BigDecimal amount) {
        String operationId = "transaction_" + System.nanoTime();
        String operationName = "banking.transaction." + transactionType.toLowerCase();
        
        // Enhanced MDC context
        MDC.put("operation.id", operationId);
        MDC.put("operation.type", "transaction");
        MDC.put("transaction.type", transactionType);
        MDC.put("business.domain", "banking");
        MDC.put("account.id", accountId);
        MDC.put("transaction.amount.range", categorizeAmount(amount));
        MDC.put("service.name", serviceName);
        MDC.put("environment", environment);
        
        Timer.Sample timerSample = Timer.start(meterRegistry);
        activeTimers.put(operationId, timerSample);
        
        logger.info("Starting {} transaction for account: {}, amount: {}", 
                transactionType, accountId, amount);
        
        // Create tagged counter for this transaction type
        Counter.builder("banking.transaction")
                .description("Number of banking transactions")
                .tag("type", transactionType.toLowerCase())
                .register(meterRegistry)
                .increment();
        
        return new BankingSpan(operationId, operationName, timerSample);
    }

    public BankingSpan startDatabaseOperation(String operation, String table) {
        String operationId = "db_" + System.nanoTime();
        String operationName = "banking.database." + operation.toLowerCase();
        
        MDC.put("operation.id", operationId);
        MDC.put("operation.type", "database");
        MDC.put("db.operation", operation);
        MDC.put("db.table", table);
        MDC.put("service.name", serviceName);
        MDC.put("environment", environment);
        
        Timer.Sample timerSample = Timer.start(meterRegistry);
        activeTimers.put(operationId, timerSample);
        
        logger.debug("Starting database operation: {} on table: {}", operation, table);
        
        return new BankingSpan(operationId, operationName, timerSample);
    }

    public BankingSpan startKafkaOperation(String operation, String topic) {
        String operationId = "kafka_" + System.nanoTime();
        String operationName = "banking.kafka." + operation.toLowerCase();
        
        MDC.put("operation.id", operationId);
        MDC.put("operation.type", "kafka");
        MDC.put("kafka.operation", operation);
        MDC.put("kafka.topic", topic);
        MDC.put("service.name", serviceName);
        MDC.put("environment", environment);
        
        Timer.Sample timerSample = Timer.start(meterRegistry);
        activeTimers.put(operationId, timerSample);
        
        logger.debug("Starting Kafka operation: {} on topic: {}", operation, topic);
        
        return new BankingSpan(operationId, operationName, timerSample);
    }

    public void finishSpan(BankingSpan span) {
        if (span == null || span.operationId == null) {
            logger.warn("Attempted to finish null or invalid span");
            return;
        }

        try {
            // Stop timer and record metrics
            Timer.Sample timerSample = activeTimers.remove(span.operationId);
            if (timerSample != null) {
                Timer timer = Timer.builder(span.operationName)
                        .description("Duration of " + span.operationName + " operation")
                        .tag("service", serviceName)
                        .tag("environment", environment)
                        .tag("operation", extractOperationType(span.operationName))
                        .register(meterRegistry);
                timerSample.stop(timer);
            }

            logger.info("Completed operation: {}", span.operationName);
        } finally {
            // Clear MDC to prevent memory leaks
            clearOperationMDC();
        }
    }

    public void finishSpanWithError(BankingSpan span, Exception error) {
        if (span == null || span.operationId == null) {
            logger.warn("Attempted to finish null or invalid span with error");
            return;
        }

        try {
            // Add error information to MDC
            MDC.put("error.type", error.getClass().getSimpleName());
            MDC.put("error.message", error.getMessage());
            
            // Stop timer and record error metrics
            Timer.Sample timerSample = activeTimers.remove(span.operationId);
            if (timerSample != null) {
                Timer timer = Timer.builder(span.operationName)
                        .description("Duration of " + span.operationName + " operation")
                        .tag("service", serviceName)
                        .tag("environment", environment)
                        .tag("operation", extractOperationType(span.operationName))
                        .tag("status", "error")
                        .register(meterRegistry);
                timerSample.stop(timer);
            }

            // Increment error counter with tags
            Counter.builder("banking.error")
                    .description("Number of banking operation errors")
                    .tag("operation", extractOperationType(span.operationName))
                    .tag("error_type", error.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();

            logger.error("Operation {} failed with error: {}", span.operationName, error.getMessage(), error);
        } finally {
            clearOperationMDC();
        }
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 8) {
            return "***";
        }
        return cpf.substring(0, 3) + "***" + cpf.substring(cpf.length() - 3);
    }

    private String categorizeAmount(BigDecimal amount) {
        if (amount == null) return "unknown";
        
        BigDecimal value = amount.abs();
        if (value.compareTo(new BigDecimal("100")) < 0) {
            return "small";
        } else if (value.compareTo(new BigDecimal("1000")) < 0) {
            return "medium";
        } else if (value.compareTo(new BigDecimal("10000")) < 0) {
            return "large";
        } else {
            return "very_large";
        }
    }

    private String extractOperationType(String operationName) {
        if (operationName == null) return "unknown";
        String[] parts = operationName.split("\\.");
        return parts.length > 2 ? parts[2] : "unknown";
    }

    private void clearOperationMDC() {
        MDC.remove("operation.id");
        MDC.remove("operation.type");
        MDC.remove("transaction.type");
        MDC.remove("business.domain");
        MDC.remove("customer.cpf.masked");
        MDC.remove("account.id");
        MDC.remove("transaction.amount.range");
        MDC.remove("db.operation");
        MDC.remove("db.table");
        MDC.remove("kafka.operation");
        MDC.remove("kafka.topic");
        MDC.remove("error.type");
        MDC.remove("error.message");
        MDC.remove("service.name");
        MDC.remove("environment");
    }

    public static class BankingSpan {
        public final String operationId;
        public final String operationName;
        private final Timer.Sample timerSample;
        private final Instant startTime;

        public BankingSpan(String operationId, String operationName, Timer.Sample timerSample) {
            this.operationId = operationId;
            this.operationName = operationName;
            this.timerSample = timerSample;
            this.startTime = Instant.now();
        }

        public Instant getStartTime() {
            return startTime;
        }
    }
}