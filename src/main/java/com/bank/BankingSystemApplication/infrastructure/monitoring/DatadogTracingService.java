package com.bank.BankingSystemApplication.infrastructure.monitoring;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class DatadogTracingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatadogTracingService.class);
    
    private final Tracer tracer;
    
    @Autowired
    public DatadogTracingService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public Span startBankingOperation(String operationType, String accountId) {
        return startBankingOperation(operationType, accountId, null);
    }
    
    public Span startBankingOperation(String operationType, String accountId, BigDecimal amount) {
        Span span = tracer.nextSpan()
                .name("banking.operation." + operationType.toLowerCase())
                .tag("operation.type", operationType)
                .tag("account.id", accountId)
                .tag("service.name", "banking-system")
                .tag("business.domain", "banking");
        
        if (amount != null) {
            span.tag("transaction.amount", amount.toString());
        }
        
        span.start();
        
        // Adiciona informações de trace ao MDC para logs correlacionados
        updateMDCWithTraceInfo(span);
        
        logger.debug("Started banking operation trace: {} for account: {}", operationType, accountId);
        return span;
    }
    
    public Span startSagaOperation(String sagaType, String transactionId) {
        Span span = tracer.nextSpan()
                .name("banking.saga." + sagaType.toLowerCase())
                .tag("saga.type", sagaType)
                .tag("transaction.id", transactionId)
                .tag("service.name", "banking-system")
                .tag("business.domain", "banking")
                .tag("pattern", "saga");
        
        span.start();
        updateMDCWithTraceInfo(span);
        
        logger.debug("Started saga operation trace: {} for transaction: {}", sagaType, transactionId);
        return span;
    }
    
    public Span startKafkaOperation(String operationType, String topic) {
        Span span = tracer.nextSpan()
                .name("banking.messaging." + operationType.toLowerCase())
                .tag("messaging.operation", operationType)
                .tag("messaging.destination", topic)
                .tag("messaging.system", "kafka")
                .tag("service.name", "banking-system");
        
        span.start();
        updateMDCWithTraceInfo(span);
        
        logger.debug("Started Kafka operation trace: {} on topic: {}", operationType, topic);
        return span;
    }
    
    public Span startDatabaseOperation(String operationType, String entity) {
        Span span = tracer.nextSpan()
                .name("banking.database." + operationType.toLowerCase())
                .tag("db.operation", operationType)
                .tag("db.table", entity)
                .tag("db.system", "postgresql")
                .tag("service.name", "banking-system");
        
        span.start();
        updateMDCWithTraceInfo(span);
        
        logger.debug("Started database operation trace: {} on entity: {}", operationType, entity);
        return span;
    }
    
    public void tagSpanWithBusinessInfo(Span span, Map<String, String> businessTags) {
        if (span != null && businessTags != null) {
            businessTags.forEach(span::tag);
        }
    }
    
    public void tagSpanWithError(Span span, Throwable error) {
        if (span != null && error != null) {
            span.tag("error", "true")
                .tag("error.kind", error.getClass().getSimpleName())
                .tag("error.message", error.getMessage() != null ? error.getMessage() : "Unknown error");
        }
    }
    
    public void tagSpanWithPerformance(Span span, long durationMs) {
        if (span != null) {
            span.tag("performance.duration_ms", String.valueOf(durationMs));
            
            // Adiciona tags de performance baseadas na duração
            if (durationMs > 5000) {
                span.tag("performance.level", "very_slow");
            } else if (durationMs > 1000) {
                span.tag("performance.level", "slow");
            } else if (durationMs > 500) {
                span.tag("performance.level", "normal");
            } else {
                span.tag("performance.level", "fast");
            }
        }
    }
    
    public void finishSpan(Span span) {
        if (span != null) {
            span.end();
            clearMDCTraceInfo();
        }
    }
    
    public void finishSpanWithError(Span span, Throwable error) {
        if (span != null) {
            tagSpanWithError(span, error);
            span.end();
            clearMDCTraceInfo();
            
            logger.error("Banking operation failed with error: {}", error.getMessage(), error);
        }
    }
    
    private void updateMDCWithTraceInfo(Span span) {
        if (span != null && span.context() != null) {
            String traceId = span.context().traceId();
            String spanId = span.context().spanId();
            
            if (traceId != null) {
                MDC.put("dd.trace_id", traceId);
                MDC.put("traceId", traceId);
            }
            
            if (spanId != null) {
                MDC.put("dd.span_id", spanId);
                MDC.put("spanId", spanId);
            }
        }
    }
    
    private void clearMDCTraceInfo() {
        MDC.remove("dd.trace_id");
        MDC.remove("dd.span_id");
        MDC.remove("traceId");
        MDC.remove("spanId");
    }
    
    // Métodos de conveniência para operações bancárias específicas
    public Span startAccountCreation(String accountId) {
        return startBankingOperation("CREATE_ACCOUNT", accountId);
    }
    
    public Span startTransaction(String accountId, BigDecimal amount, String transactionType) {
        Span span = startBankingOperation("TRANSACTION", accountId, amount);
        span.tag("transaction.type", transactionType);
        return span;
    }
    
    public Span startTransfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        Span span = tracer.nextSpan()
                .name("banking.operation.transfer")
                .tag("operation.type", "TRANSFER")
                .tag("account.from", fromAccountId)
                .tag("account.to", toAccountId)
                .tag("transaction.amount", amount.toString())
                .tag("service.name", "banking-system")
                .tag("business.domain", "banking");
        
        span.start();
        updateMDCWithTraceInfo(span);
        
        logger.debug("Started transfer trace from {} to {} amount {}", fromAccountId, toAccountId, amount);
        return span;
    }
}