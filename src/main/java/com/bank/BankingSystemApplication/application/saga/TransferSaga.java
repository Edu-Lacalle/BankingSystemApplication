package com.bank.BankingSystemApplication.application.saga;

import com.bank.BankingSystemApplication.infrastructure.audit.BankingAuditService;
import com.bank.BankingSystemApplication.domain.model.Status;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.infrastructure.monitoring.BankingMetricsService;
import com.bank.BankingSystemApplication.application.service.ResilientAccountService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementação do padrão Saga para transferências entre contas
 * Gerencia transações distribuídas com compensação automática em caso de falha
 */
@Component
public class TransferSaga {
    
    private static final Logger logger = LoggerFactory.getLogger(TransferSaga.class);
    
    @Autowired
    private ResilientAccountService resilientAccountService;
    
    @Autowired
    private BankingMetricsService metricsService;
    
    @Autowired
    private BankingAuditService auditService;
    
    /**
     * Executa uma transferência entre contas usando o padrão Saga
     */
    public SagaResult executeTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        String sagaId = UUID.randomUUID().toString();
        MDC.put("correlationId", sagaId);
        MDC.put("operation", "executeTransfer");
        MDC.put("fromAccountId", String.valueOf(fromAccountId));
        MDC.put("toAccountId", String.valueOf(toAccountId));
        
        logger.info("Iniciando Saga de transferência: {} de {} para {} no valor de {}", 
                   sagaId, fromAccountId, toAccountId, amount);
        
        SagaResult result = new SagaResult(sagaId, fromAccountId, toAccountId, amount);
        Timer.Sample sagaSample = metricsService.startSagaTimer();
        
        try {
            metricsService.incrementTransferInitiated();
            auditService.auditSagaTransfer(sagaId, fromAccountId, toAccountId, amount, 
                                         "STARTED", "INIT", "Saga iniciada");
            
            // Etapa 1: Débito da conta origem
            result = executeDebitStep(result);
            
            if (!result.isDebitSuccessful()) {
                logger.warn("Saga {} falhou na etapa de débito", sagaId);
                metricsService.incrementTransferFailed();
                auditService.auditSagaTransfer(sagaId, fromAccountId, toAccountId, amount, 
                                             "FAILED", "DEBIT", "Falha no débito: " + result.getErrorMessage());
                return result;
            }
            
            // Etapa 2: Crédito da conta destino
            result = executeCreditStep(result);
            
            if (!result.isCreditSuccessful()) {
                logger.warn("Saga {} falhou na etapa de crédito, iniciando compensação", sagaId);
                auditService.auditSagaTransfer(sagaId, fromAccountId, toAccountId, amount, 
                                             "COMPENSATING", "CREDIT", "Falha no crédito, iniciando compensação");
                // Compensação: reverter o débito
                result = executeCompensation(result);
                
                if (result.isCompensationSuccessful()) {
                    metricsService.incrementSagaCompensated();
                    auditService.auditSagaTransfer(sagaId, fromAccountId, toAccountId, amount, 
                                                 "COMPENSATED", "COMPENSATION", "Compensação executada com sucesso");
                } else {
                    auditService.auditSagaTransfer(sagaId, fromAccountId, toAccountId, amount, 
                                                 "COMPENSATION_FAILED", "COMPENSATION", "Falha na compensação");
                }
            } else {
                metricsService.incrementTransferCompleted();
                auditService.auditSagaTransfer(sagaId, fromAccountId, toAccountId, amount, 
                                             "COMPLETED", "FINAL", "Transferência concluída com sucesso");
            }
            
            metricsService.recordSagaTime(sagaSample);
            logger.info("Saga {} finalizada com status: {}", sagaId, result.getOverallStatus());
            return result;
            
        } catch (Exception e) {
            logger.error("Erro crítico na Saga {}: {}", sagaId, e.getMessage(), e);
            result.setOverallStatus(SagaStatus.FAILED);
            result.setErrorMessage("Erro crítico: " + e.getMessage());
            
            metricsService.incrementTransferFailed();
            metricsService.recordSagaTime(sagaSample);
            auditService.auditSystemFailure("TransferSaga", "executeTransfer", 
                                           e.getMessage(), e.getStackTrace().toString(), sagaId);
            
            // Tentar compensação mesmo em caso de erro crítico
            if (result.isDebitSuccessful()) {
                executeCompensation(result);
            }
            
            return result;
        } finally {
            MDC.clear();
        }
    }
    
    private SagaResult executeDebitStep(SagaResult result) {
        logger.info("Executando etapa de débito para Saga: {}", result.getSagaId());
        
        try {
            TransactionRequest debitRequest = new TransactionRequest();
            debitRequest.setAccountId(result.getFromAccountId());
            debitRequest.setAmount(result.getAmount());
            
            TransactionResponse debitResponse = resilientAccountService.debitResilient(debitRequest);
            
            result.setDebitResponse(debitResponse);
            result.setDebitTimestamp(LocalDateTime.now());
            
            if (debitResponse.getStatus() == Status.EFETUADO) {
                result.setDebitSuccessful(true);
                auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                             result.getToAccountId(), result.getAmount(), 
                                             "IN_PROGRESS", "DEBIT", "Débito executado com sucesso");
                logger.info("Débito executado com sucesso na Saga: {}", result.getSagaId());
            } else {
                result.setDebitSuccessful(false);
                result.setOverallStatus(SagaStatus.FAILED);
                result.setErrorMessage("Falha no débito: " + debitResponse.getMessage());
                auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                             result.getToAccountId(), result.getAmount(), 
                                             "FAILED", "DEBIT", "Falha no débito: " + debitResponse.getMessage());
                logger.warn("Falha no débito da Saga: {} - {}", result.getSagaId(), debitResponse.getMessage());
            }
            
        } catch (Exception e) {
            result.setDebitSuccessful(false);
            result.setOverallStatus(SagaStatus.FAILED);
            result.setErrorMessage("Erro no débito: " + e.getMessage());
            auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                         result.getToAccountId(), result.getAmount(), 
                                         "FAILED", "DEBIT", "Erro no débito: " + e.getMessage());
            logger.error("Erro no débito da Saga: {} - {}", result.getSagaId(), e.getMessage(), e);
        }
        
        return result;
    }
    
    private SagaResult executeCreditStep(SagaResult result) {
        logger.info("Executando etapa de crédito para Saga: {}", result.getSagaId());
        
        try {
            TransactionRequest creditRequest = new TransactionRequest();
            creditRequest.setAccountId(result.getToAccountId());
            creditRequest.setAmount(result.getAmount());
            
            TransactionResponse creditResponse = resilientAccountService.creditResilient(creditRequest);
            
            result.setCreditResponse(creditResponse);
            result.setCreditTimestamp(LocalDateTime.now());
            
            if (creditResponse.getStatus() == Status.EFETUADO) {
                result.setCreditSuccessful(true);
                result.setOverallStatus(SagaStatus.COMPLETED);
                auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                             result.getToAccountId(), result.getAmount(), 
                                             "COMPLETED", "CREDIT", "Crédito executado com sucesso");
                logger.info("Crédito executado com sucesso na Saga: {}", result.getSagaId());
            } else {
                result.setCreditSuccessful(false);
                result.setOverallStatus(SagaStatus.COMPENSATING);
                result.setErrorMessage("Falha no crédito: " + creditResponse.getMessage());
                auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                             result.getToAccountId(), result.getAmount(), 
                                             "COMPENSATING", "CREDIT", "Falha no crédito: " + creditResponse.getMessage());
                logger.warn("Falha no crédito da Saga: {} - {}", result.getSagaId(), creditResponse.getMessage());
            }
            
        } catch (Exception e) {
            result.setCreditSuccessful(false);
            result.setOverallStatus(SagaStatus.COMPENSATING);
            result.setErrorMessage("Erro no crédito: " + e.getMessage());
            auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                         result.getToAccountId(), result.getAmount(), 
                                         "COMPENSATING", "CREDIT", "Erro no crédito: " + e.getMessage());
            logger.error("Erro no crédito da Saga: {} - {}", result.getSagaId(), e.getMessage(), e);
        }
        
        return result;
    }
    
    private SagaResult executeCompensation(SagaResult result) {
        logger.info("Executando compensação para Saga: {}", result.getSagaId());
        
        try {
            // Reverter o débito fazendo um crédito na conta origem
            TransactionRequest compensationRequest = new TransactionRequest();
            compensationRequest.setAccountId(result.getFromAccountId());
            compensationRequest.setAmount(result.getAmount());
            
            TransactionResponse compensationResponse = resilientAccountService.creditResilient(compensationRequest);
            
            result.setCompensationResponse(compensationResponse);
            result.setCompensationTimestamp(LocalDateTime.now());
            
            if (compensationResponse.getStatus() == Status.EFETUADO) {
                result.setCompensationSuccessful(true);
                result.setOverallStatus(SagaStatus.COMPENSATED);
                auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                             result.getToAccountId(), result.getAmount(), 
                                             "COMPENSATED", "COMPENSATION", "Compensação executada com sucesso");
                logger.info("Compensação executada com sucesso na Saga: {}", result.getSagaId());
            } else {
                result.setCompensationSuccessful(false);
                result.setOverallStatus(SagaStatus.COMPENSATION_FAILED);
                auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                             result.getToAccountId(), result.getAmount(), 
                                             "COMPENSATION_FAILED", "COMPENSATION", 
                                             "Falha na compensação: " + compensationResponse.getMessage());
                logger.error("Falha na compensação da Saga: {} - {}", result.getSagaId(), compensationResponse.getMessage());
            }
            
        } catch (Exception e) {
            result.setCompensationSuccessful(false);
            result.setOverallStatus(SagaStatus.COMPENSATION_FAILED);
            auditService.auditSagaTransfer(result.getSagaId(), result.getFromAccountId(), 
                                         result.getToAccountId(), result.getAmount(), 
                                         "COMPENSATION_FAILED", "COMPENSATION", 
                                         "Erro crítico na compensação: " + e.getMessage());
            logger.error("Erro crítico na compensação da Saga: {} - {}", result.getSagaId(), e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Status possíveis da Saga
     */
    public enum SagaStatus {
        STARTED,
        COMPLETED,
        FAILED,
        COMPENSATING,
        COMPENSATED,
        COMPENSATION_FAILED
    }
    
    /**
     * Resultado da execução da Saga
     */
    public static class SagaResult {
        private final String sagaId;
        private final Long fromAccountId;
        private final Long toAccountId;
        private final BigDecimal amount;
        private final LocalDateTime startTime;
        
        private SagaStatus overallStatus = SagaStatus.STARTED;
        private String errorMessage;
        
        // Etapa de débito
        private boolean debitSuccessful = false;
        private TransactionResponse debitResponse;
        private LocalDateTime debitTimestamp;
        
        // Etapa de crédito
        private boolean creditSuccessful = false;
        private TransactionResponse creditResponse;
        private LocalDateTime creditTimestamp;
        
        // Compensação
        private boolean compensationSuccessful = false;
        private TransactionResponse compensationResponse;
        private LocalDateTime compensationTimestamp;
        
        public SagaResult(String sagaId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
            this.sagaId = sagaId;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.startTime = LocalDateTime.now();
        }
        
        // Getters e Setters
        public String getSagaId() { return sagaId; }
        public Long getFromAccountId() { return fromAccountId; }
        public Long getToAccountId() { return toAccountId; }
        public BigDecimal getAmount() { return amount; }
        public LocalDateTime getStartTime() { return startTime; }
        
        public SagaStatus getOverallStatus() { return overallStatus; }
        public void setOverallStatus(SagaStatus overallStatus) { this.overallStatus = overallStatus; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public boolean isDebitSuccessful() { return debitSuccessful; }
        public void setDebitSuccessful(boolean debitSuccessful) { this.debitSuccessful = debitSuccessful; }
        
        public TransactionResponse getDebitResponse() { return debitResponse; }
        public void setDebitResponse(TransactionResponse debitResponse) { this.debitResponse = debitResponse; }
        
        public LocalDateTime getDebitTimestamp() { return debitTimestamp; }
        public void setDebitTimestamp(LocalDateTime debitTimestamp) { this.debitTimestamp = debitTimestamp; }
        
        public boolean isCreditSuccessful() { return creditSuccessful; }
        public void setCreditSuccessful(boolean creditSuccessful) { this.creditSuccessful = creditSuccessful; }
        
        public TransactionResponse getCreditResponse() { return creditResponse; }
        public void setCreditResponse(TransactionResponse creditResponse) { this.creditResponse = creditResponse; }
        
        public LocalDateTime getCreditTimestamp() { return creditTimestamp; }
        public void setCreditTimestamp(LocalDateTime creditTimestamp) { this.creditTimestamp = creditTimestamp; }
        
        public boolean isCompensationSuccessful() { return compensationSuccessful; }
        public void setCompensationSuccessful(boolean compensationSuccessful) { this.compensationSuccessful = compensationSuccessful; }
        
        public TransactionResponse getCompensationResponse() { return compensationResponse; }
        public void setCompensationResponse(TransactionResponse compensationResponse) { this.compensationResponse = compensationResponse; }
        
        public LocalDateTime getCompensationTimestamp() { return compensationTimestamp; }
        public void setCompensationTimestamp(LocalDateTime compensationTimestamp) { this.compensationTimestamp = compensationTimestamp; }
        
        public boolean isSuccessful() {
            return overallStatus == SagaStatus.COMPLETED;
        }
        
        @Override
        public String toString() {
            return "SagaResult{" +
                    "sagaId='" + sagaId + '\'' +
                    ", fromAccountId=" + fromAccountId +
                    ", toAccountId=" + toAccountId +
                    ", amount=" + amount +
                    ", overallStatus=" + overallStatus +
                    ", debitSuccessful=" + debitSuccessful +
                    ", creditSuccessful=" + creditSuccessful +
                    ", compensationSuccessful=" + compensationSuccessful +
                    '}';
        }
    }
}