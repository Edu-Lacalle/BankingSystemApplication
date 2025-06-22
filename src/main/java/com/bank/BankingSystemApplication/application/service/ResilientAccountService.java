package com.bank.BankingSystemApplication.application.service;

import com.bank.BankingSystemApplication.infrastructure.audit.BankingAuditService;
import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.infrastructure.monitoring.BankingMetricsService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço resiliente que implementa patterns de Circuit Breaker, Retry, Rate Limiting e Timeout
 * para operações bancárias críticas
 */
@Service
public class ResilientAccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientAccountService.class);
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private BankingMetricsService metricsService;
    
    @Autowired
    private BankingAuditService auditService;
    
    /**
     * Criação de conta com Circuit Breaker e Retry
     */
    @CircuitBreaker(name = "banking-service", fallbackMethod = "fallbackCreateAccount")
    @Retry(name = "banking-service")
    @TimeLimiter(name = "banking-service")
    public CompletableFuture<Account> createAccountResilient(AccountCreationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "createAccountResilient");
        MDC.put("cpf", request.getCpf().substring(0, 3) + "*****" + request.getCpf().substring(8));
        
        logger.info("Tentando criar conta resiliente para CPF: {}", request.getCpf().substring(0, 3) + "***");
        
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = metricsService.startAccountCreationTimer();
            
            try {
                // Simula possível latência ou falha
                if (Math.random() > 0.7) { // 30% de chance de falha para demonstrar o pattern
                    logger.warn("Simulando falha temporária na criação de conta");
                    auditService.auditCircuitBreakerEvent("banking-service", "FAILURE_SIMULATION", 
                                                         "createAccountResilient", "Falha temporária simulada");
                    throw new RuntimeException("Falha temporária no sistema");
                }
                
                Account account = accountService.createAccount(request);
                
                metricsService.recordAccountCreationTime(sample);
                auditService.auditCircuitBreakerEvent("banking-service", "SUCCESS", 
                                                     "createAccountResilient", "Conta criada com sucesso");
                
                logger.info("Conta criada com sucesso de forma resiliente: {}", account.getId());
                return account;
                
            } catch (Exception e) {
                metricsService.recordAccountCreationTime(sample);
                auditService.auditSystemFailure("ResilientAccountService", "createAccountResilient", 
                                               e.getMessage(), e.getStackTrace().toString(), correlationId);
                logger.error("Erro na criação resiliente de conta: {}", e.getMessage());
                throw e;
            } finally {
                MDC.clear();
            }
        });
    }
    
    /**
     * Operação de crédito com Circuit Breaker e Rate Limiting
     */
    @CircuitBreaker(name = "banking-service", fallbackMethod = "fallbackCreditOperation")
    @RateLimiter(name = "banking-api")
    @Retry(name = "banking-service")
    public TransactionResponse creditResilient(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "creditResilient");
        MDC.put("accountId", String.valueOf(request.getAccountId()));
        
        logger.info("Executando operação de crédito resiliente para conta: {}", request.getAccountId());
        
        Timer.Sample sample = metricsService.startTransactionTimer();
        
        try {
            // Simula possível latência
            if (Math.random() > 0.8) { // 20% de chance de falha
                logger.warn("Simulando falha temporária na operação de crédito");
                auditService.auditCircuitBreakerEvent("banking-service", "FAILURE_SIMULATION", 
                                                     "creditResilient", "Falha temporária simulada");
                throw new RuntimeException("Falha temporária no sistema de pagamento");
            }
            
            TransactionResponse response = accountService.credit(request);
            
            metricsService.recordTransactionTime(sample);
            auditService.auditCircuitBreakerEvent("banking-service", "SUCCESS", 
                                                 "creditResilient", "Crédito executado com sucesso");
            
            logger.info("Operação de crédito resiliente executada com sucesso");
            return response;
            
        } catch (Exception e) {
            metricsService.recordTransactionTime(sample);
            auditService.auditSystemFailure("ResilientAccountService", "creditResilient", 
                                           e.getMessage(), e.getStackTrace().toString(), correlationId);
            logger.error("Erro na operação de crédito resiliente: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Operação de débito com Circuit Breaker e Rate Limiting
     */
    @CircuitBreaker(name = "banking-service", fallbackMethod = "fallbackDebitOperation")
    @RateLimiter(name = "banking-api")
    @Retry(name = "banking-service")
    public TransactionResponse debitResilient(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "debitResilient");
        MDC.put("accountId", String.valueOf(request.getAccountId()));
        
        logger.info("Executando operação de débito resiliente para conta: {}", request.getAccountId());
        
        Timer.Sample sample = metricsService.startTransactionTimer();
        
        try {
            // Simula possível latência
            if (Math.random() > 0.8) { // 20% de chance de falha
                logger.warn("Simulando falha temporária na operação de débito");
                auditService.auditCircuitBreakerEvent("banking-service", "FAILURE_SIMULATION", 
                                                     "debitResilient", "Falha temporária simulada");
                throw new RuntimeException("Falha temporária no sistema de pagamento");
            }
            
            TransactionResponse response = accountService.debit(request);
            
            metricsService.recordTransactionTime(sample);
            auditService.auditCircuitBreakerEvent("banking-service", "SUCCESS", 
                                                 "debitResilient", "Débito executado com sucesso");
            
            logger.info("Operação de débito resiliente executada com sucesso");
            return response;
            
        } catch (Exception e) {
            metricsService.recordTransactionTime(sample);
            auditService.auditSystemFailure("ResilientAccountService", "debitResilient", 
                                           e.getMessage(), e.getStackTrace().toString(), correlationId);
            logger.error("Erro na operação de débito resiliente: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    // Métodos de fallback
    
    public CompletableFuture<Account> fallbackCreateAccount(AccountCreationRequest request, Exception ex) {
        logger.error("Circuit Breaker ativado para criação de conta. Causa: {}", ex.getMessage());
        
        auditService.auditCircuitBreakerEvent("banking-service", "OPEN", 
                                             "createAccountResilient", "Circuit Breaker ativado: " + ex.getMessage());
        
        return CompletableFuture.failedFuture(
            new RuntimeException("Serviço temporariamente indisponível. Tente novamente em alguns minutos.")
        );
    }
    
    public TransactionResponse fallbackCreditOperation(TransactionRequest request, Exception ex) {
        logger.error("Circuit Breaker ativado para operação de crédito. Conta: {}, Causa: {}", 
                    request.getAccountId(), ex.getMessage());
        
        auditService.auditCircuitBreakerEvent("banking-service", "OPEN", 
                                             "creditResilient", "Circuit Breaker ativado: " + ex.getMessage());
        
        return new TransactionResponse(
            com.bank.BankingSystemApplication.domain.model.Status.RECUSADO,
            "Serviço temporariamente indisponível. Sua transação será processada assim que o sistema for restabelecido."
        );
    }
    
    public TransactionResponse fallbackDebitOperation(TransactionRequest request, Exception ex) {
        logger.error("Circuit Breaker ativado para operação de débito. Conta: {}, Causa: {}", 
                    request.getAccountId(), ex.getMessage());
        
        auditService.auditCircuitBreakerEvent("banking-service", "OPEN", 
                                             "debitResilient", "Circuit Breaker ativado: " + ex.getMessage());
        
        return new TransactionResponse(
            com.bank.BankingSystemApplication.domain.model.Status.RECUSADO,
            "Serviço temporariamente indisponível. Sua transação será processada assim que o sistema for restabelecido."
        );
    }
}