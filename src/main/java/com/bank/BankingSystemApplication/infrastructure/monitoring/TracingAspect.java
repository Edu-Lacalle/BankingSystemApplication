package com.bank.BankingSystemApplication.infrastructure.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * AOP Aspect for automatic tracing of banking operations.
 * This aspect automatically adds distributed tracing to key business operations
 * without requiring manual instrumentation in business logic.
 */
@Aspect
@Component
public class TracingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(TracingAspect.class);
    
    @Autowired
    private DatadogTracingService tracingService;
    
    /**
     * Traces account creation operations.
     */
    @Around("execution(* com.bank.BankingSystemApplication.domain.service.BankingDomainService.createAccount(..))")
    public Object traceAccountCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String cpf = null;
        
        // Extract CPF from AccountCreationRequest
        if (args.length > 0 && args[0] != null) {
            try {
                cpf = (String) args[0].getClass().getMethod("getCpf").invoke(args[0]);
            } catch (Exception e) {
                logger.debug("Could not extract CPF from arguments", e);
            }
        }
        
        DatadogTracingService.BankingSpan span = tracingService.startAccountCreation(cpf != null ? cpf : "unknown");
        
        try {
            Object result = joinPoint.proceed();
            tracingService.finishSpan(span);
            return result;
        } catch (Exception e) {
            tracingService.finishSpanWithError(span, e);
            throw e;
        }
    }
    
    /**
     * Traces credit operations.
     */
    @Around("execution(* com.bank.BankingSystemApplication.domain.service.BankingDomainService.credit(..))")
    public Object traceCreditOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String accountId = "unknown";
        BigDecimal amount = BigDecimal.ZERO;
        
        // Extract parameters from TransactionRequest
        if (args.length > 0 && args[0] != null) {
            try {
                Object request = args[0];
                accountId = String.valueOf(request.getClass().getMethod("getAccountId").invoke(request));
                amount = (BigDecimal) request.getClass().getMethod("getAmount").invoke(request);
            } catch (Exception e) {
                logger.debug("Could not extract transaction parameters", e);
            }
        }
        
        DatadogTracingService.BankingSpan span = tracingService.startTransaction(accountId, "CREDIT", amount);
        
        try {
            Object result = joinPoint.proceed();
            tracingService.finishSpan(span);
            return result;
        } catch (Exception e) {
            tracingService.finishSpanWithError(span, e);
            throw e;
        }
    }
    
    /**
     * Traces debit operations.
     */
    @Around("execution(* com.bank.BankingSystemApplication.domain.service.BankingDomainService.debit(..))")
    public Object traceDebitOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String accountId = "unknown";
        BigDecimal amount = BigDecimal.ZERO;
        
        // Extract parameters from TransactionRequest
        if (args.length > 0 && args[0] != null) {
            try {
                Object request = args[0];
                accountId = String.valueOf(request.getClass().getMethod("getAccountId").invoke(request));
                amount = (BigDecimal) request.getClass().getMethod("getAmount").invoke(request);
            } catch (Exception e) {
                logger.debug("Could not extract transaction parameters", e);
            }
        }
        
        DatadogTracingService.BankingSpan span = tracingService.startTransaction(accountId, "DEBIT", amount);
        
        try {
            Object result = joinPoint.proceed();
            tracingService.finishSpan(span);
            return result;
        } catch (Exception e) {
            tracingService.finishSpanWithError(span, e);
            throw e;
        }
    }
    
    /**
     * Traces database operations.
     */
    @Around("execution(* com.bank.BankingSystemApplication.infrastructure.persistence.AccountRepository.*(..))")
    public Object traceDatabaseOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String operation = mapMethodToOperation(methodName);
        
        DatadogTracingService.BankingSpan span = tracingService.startDatabaseOperation(operation, "accounts");
        
        try {
            Object result = joinPoint.proceed();
            // tracingService.addSpanAttribute(span, "db.rows_affected", getRowCount(result));
            tracingService.finishSpan(span);
            return result;
        } catch (Exception e) {
            tracingService.finishSpanWithError(span, e);
            throw e;
        }
    }
    
    /**
     * Traces Kafka producer operations.
     */
    @Around("execution(* com.bank.BankingSystemApplication.application.service.kafka.TransactionEventProducer.*(..))")
    public Object traceKafkaProducer(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String topic = extractTopicFromArgs(args);
        
        DatadogTracingService.BankingSpan span = tracingService.startKafkaOperation("PRODUCE", topic);
        
        try {
            Object result = joinPoint.proceed();
            // tracingService.addSpanAttribute(span, "kafka.message_key", extractMessageKey(args));
            tracingService.finishSpan(span);
            return result;
        } catch (Exception e) {
            tracingService.finishSpanWithError(span, e);
            throw e;
        }
    }
    
    /**
     * Traces Kafka consumer operations.
     */
    @Around("execution(* com.bank.BankingSystemApplication.application.service.kafka.*Consumer.*(..))")
    public Object traceKafkaConsumer(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String topic = extractTopicFromConsumerClass(className);
        
        DatadogTracingService.BankingSpan span = tracingService.startKafkaOperation("CONSUME", topic);
        
        try {
            Object result = joinPoint.proceed();
            tracingService.finishSpan(span);
            return result;
        } catch (Exception e) {
            tracingService.finishSpanWithError(span, e);
            throw e;
        }
    }
    
    /**
     * Maps repository method names to database operations.
     */
    private String mapMethodToOperation(String methodName) {
        if (methodName.startsWith("save") || methodName.startsWith("persist")) {
            return "INSERT";
        } else if (methodName.startsWith("find") || methodName.startsWith("get") || methodName.startsWith("select")) {
            return "SELECT";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else {
            return methodName.toUpperCase();
        }
    }
    
    /**
     * Extracts row count from database operation results.
     */
    private String getRowCount(Object result) {
        if (result == null) {
            return "0";
        } else if (result instanceof java.util.Collection) {
            return String.valueOf(((java.util.Collection<?>) result).size());
        } else if (result instanceof java.util.Optional) {
            return ((java.util.Optional<?>) result).isPresent() ? "1" : "0";
        } else {
            return "1";
        }
    }
    
    /**
     * Extracts Kafka topic from method arguments.
     */
    private String extractTopicFromArgs(Object[] args) {
        // Try to extract topic from event or message arguments
        for (Object arg : args) {
            if (arg != null) {
                String className = arg.getClass().getSimpleName();
                if (className.contains("Event")) {
                    if (className.contains("Transaction")) {
                        return "banking.transactions";
                    } else if (className.contains("Account")) {
                        return "banking.accounts";
                    } else if (className.contains("Notification")) {
                        return "banking.notifications";
                    }
                }
            }
        }
        return "unknown";
    }
    
    /**
     * Extracts message key from Kafka producer arguments.
     */
    private String extractMessageKey(Object[] args) {
        // Try to extract key from arguments - usually the first string argument
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return "unknown";
    }
    
    /**
     * Extracts topic from consumer class name.
     */
    private String extractTopicFromConsumerClass(String className) {
        if (className.contains("Transaction")) {
            return "banking.transactions";
        } else if (className.contains("Account")) {
            return "banking.accounts";
        } else if (className.contains("Notification")) {
            return "banking.notifications";
        } else {
            return "unknown";
        }
    }
}