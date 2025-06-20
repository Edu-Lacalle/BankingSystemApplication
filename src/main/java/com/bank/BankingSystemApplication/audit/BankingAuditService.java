package com.bank.BankingSystemApplication.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço de auditoria para operações bancárias.
 * 
 * Este serviço é responsável por registrar todos os eventos críticos 
 * do sistema bancário para fins de:
 * - Conformidade regulatória
 * - Investigação de incidentes
 * - Análise de segurança
 * - Rastreabilidade de operações
 * 
 * Tipos de eventos auditados:
 * - Criação de contas
 * - Transações financeiras (crédito/débito)
 * - Transferências via Saga
 * - Eventos de segurança
 * - Falhas de sistema
 * - Operações de Circuit Breaker
 * - Eventos Kafka
 * 
 * Características de segurança:
 * - Mascaramento de dados sensíveis (CPF)
 * - Logs estruturados em JSON
 * - Correlation IDs para rastreamento
 * - Timestamps precisos
 * - Logging separado para auditoria
 * 
 * @author Sistema Bancário
 * @version 1.0
 * @since 1.0
 */
@Service
public class BankingAuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("com.bank.BankingSystemApplication.audit");
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Audita criação de conta
     */
    public void auditAccountCreation(Long accountId, String cpf, String userName, boolean success, String details) {
        Map<String, Object> auditData = createBaseAuditData("ACCOUNT_CREATION");
        auditData.put("account_id", accountId);
        auditData.put("cpf", maskCpf(cpf));
        auditData.put("user_name", userName);
        auditData.put("success", success);
        auditData.put("details", details);
        
        logAuditEvent(auditData);
    }
    
    /**
     * Audita operações de transação
     */
    public void auditTransaction(String transactionType, Long accountId, BigDecimal amount, 
                               boolean success, String details, String correlationId) {
        Map<String, Object> auditData = createBaseAuditData("TRANSACTION");
        auditData.put("transaction_type", transactionType);
        auditData.put("account_id", accountId);
        auditData.put("amount", amount);
        auditData.put("success", success);
        auditData.put("details", details);
        auditData.put("correlation_id", correlationId);
        
        logAuditEvent(auditData);
    }
    
    /**
     * Audita transferências usando Saga
     */
    public void auditSagaTransfer(String sagaId, Long fromAccountId, Long toAccountId, 
                                BigDecimal amount, String status, String step, String details) {
        Map<String, Object> auditData = createBaseAuditData("SAGA_TRANSFER");
        auditData.put("saga_id", sagaId);
        auditData.put("from_account_id", fromAccountId);
        auditData.put("to_account_id", toAccountId);
        auditData.put("amount", amount);
        auditData.put("saga_status", status);
        auditData.put("saga_step", step);
        auditData.put("details", details);
        
        logAuditEvent(auditData);
    }
    
    /**
     * Audita eventos de segurança
     */
    public void auditSecurityEvent(String eventType, String ipAddress, String userAgent, 
                                 String details, String severity) {
        Map<String, Object> auditData = createBaseAuditData("SECURITY");
        auditData.put("event_type", eventType);
        auditData.put("ip_address", ipAddress);
        auditData.put("user_agent", userAgent);
        auditData.put("details", details);
        auditData.put("severity", severity);
        
        logAuditEvent(auditData);
    }
    
    /**
     * Audita falhas de sistema
     */
    public void auditSystemFailure(String component, String operation, String errorMessage, 
                                 String stackTrace, String correlationId) {
        Map<String, Object> auditData = createBaseAuditData("SYSTEM_FAILURE");
        auditData.put("component", component);
        auditData.put("operation", operation);
        auditData.put("error_message", errorMessage);
        auditData.put("stack_trace", stackTrace);
        auditData.put("correlation_id", correlationId);
        
        logAuditEvent(auditData);
    }
    
    /**
     * Audita operações de Circuit Breaker
     */
    public void auditCircuitBreakerEvent(String circuitBreakerName, String state, 
                                       String operation, String details) {
        Map<String, Object> auditData = createBaseAuditData("CIRCUIT_BREAKER");
        auditData.put("circuit_breaker_name", circuitBreakerName);
        auditData.put("state", state);
        auditData.put("operation", operation);
        auditData.put("details", details);
        
        logAuditEvent(auditData);
    }
    
    /**
     * Audita events Kafka
     */
    public void auditKafkaEvent(String topic, String eventType, String eventId, 
                              boolean success, String details) {
        Map<String, Object> auditData = createBaseAuditData("KAFKA_EVENT");
        auditData.put("topic", topic);
        auditData.put("event_type", eventType);
        auditData.put("event_id", eventId);
        auditData.put("success", success);
        auditData.put("details", details);
        
        logAuditEvent(auditData);
    }
    
    /**
     * Cria dados base para auditoria
     */
    private Map<String, Object> createBaseAuditData(String auditType) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("audit_id", UUID.randomUUID().toString());
        auditData.put("audit_type", auditType);
        auditData.put("timestamp", LocalDateTime.now().toString());
        auditData.put("trace_id", MDC.get("traceId"));
        auditData.put("span_id", MDC.get("spanId"));
        
        return auditData;
    }
    
    /**
     * Registra evento de auditoria no log
     */
    private void logAuditEvent(Map<String, Object> auditData) {
        try {
            // Adicionar ao MDC para contexto estruturado
            MDC.put("audit_id", (String) auditData.get("audit_id"));
            MDC.put("audit_type", (String) auditData.get("audit_type"));
            
            String auditJson = objectMapper.writeValueAsString(auditData);
            auditLogger.info("AUDIT_EVENT: {}", auditJson);
            
        } catch (Exception e) {
            auditLogger.error("Erro ao serializar dados de auditoria: {}", e.getMessage(), e);
        } finally {
            // Limpar MDC
            MDC.remove("audit_id");
            MDC.remove("audit_type");
        }
    }
    
    /**
     * Máscara CPF para auditoria (manter apenas primeiros e últimos dígitos)
     */
    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return "***";
        }
        return cpf.substring(0, 3) + "*****" + cpf.substring(8);
    }
}