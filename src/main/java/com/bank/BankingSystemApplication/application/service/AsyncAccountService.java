package com.bank.BankingSystemApplication.application.service;

import com.bank.BankingSystemApplication.infrastructure.audit.BankingAuditService;
import com.bank.BankingSystemApplication.domain.model.*;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.infrastructure.monitoring.BankingMetricsService;
import com.bank.BankingSystemApplication.application.service.kafka.TransactionEventProducer;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AsyncAccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncAccountService.class);
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionEventProducer eventProducer;
    
    @Autowired
    private BankingMetricsService metricsService;
    
    @Autowired
    private BankingAuditService auditService;
    
    public Account createAccountAsync(AccountCreationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "createAccountAsync");
        
        Timer.Sample kafkaPublishSample = null;
        if (metricsService != null) {
            kafkaPublishSample = metricsService.startKafkaPublishTimer();
        }
        
        try {
            // Criar conta de forma síncrona
            Account account = accountService.createAccount(request);
            
            // Publicar eventos assíncronos
            String eventId = UUID.randomUUID().toString();
            
            // Evento de auditoria
            TransactionEvent auditEvent = new TransactionEvent(
                eventId,
                account.getId(),
                account.getBalance(),
                TransactionType.ACCOUNT_CREATION,
                Status.EFETUADO,
                "Conta criada com sucesso"
            );
            
            // Publicar evento de auditoria
            try {
                eventProducer.publishAuditEvent(auditEvent);
                
                // Evento de notificação (se email estiver presente)
                if (account.getEmail() != null && !account.getEmail().isEmpty()) {
                    NotificationEvent notificationEvent = new NotificationEvent(
                        eventId,
                        account.getId(),
                        account.getEmail(),
                        "Bem-vindo(a) ao nosso banco! Sua conta foi criada com sucesso.",
                        NotificationType.ACCOUNT_CREATED
                    );
                    eventProducer.publishNotificationEvent(notificationEvent);
                }
            } catch (RuntimeException e) {
                // Re-propagate runtime exceptions as expected by tests
                throw e;
            }
            
            if (metricsService != null && kafkaPublishSample != null) {
                metricsService.recordKafkaPublishTime(kafkaPublishSample);
            }
            
            logger.info("Conta criada e eventos publicados para ID: {}", account.getId());
            return account;
            
        } catch (Exception e) {
            if (metricsService != null && kafkaPublishSample != null) {
                metricsService.recordKafkaPublishTime(kafkaPublishSample);
            }
            logger.error("Erro ao criar conta de forma assíncrona: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    public TransactionResponse creditAsync(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "creditAsync");
        MDC.put("accountId", String.valueOf(request.getAccountId()));
        
        Timer.Sample kafkaPublishSample = null;
        if (metricsService != null) {
            kafkaPublishSample = metricsService.startKafkaPublishTimer();
        }
        
        try {
            // Executar transação síncrona
            TransactionResponse response = accountService.credit(request);
            
            // Publicar eventos assíncronos
            String eventId = UUID.randomUUID().toString();
            
            TransactionEvent transactionEvent = new TransactionEvent(
                eventId,
                request.getAccountId(),
                request.getAmount(),
                TransactionType.CREDIT,
                response.getStatus(),
                response.getMessage()
            );
            
            // Publicar evento de transação
            eventProducer.publishTransactionEvent(transactionEvent);
            
            // Publicar evento de auditoria
            eventProducer.publishAuditEvent(transactionEvent);
            
            // Publicar notificação se transação foi bem-sucedida
            if (response.getStatus() == Status.EFETUADO) {
                Account account = accountService.getAccountById(request.getAccountId());
                if (account.getEmail() != null && !account.getEmail().isEmpty()) {
                    NotificationEvent notificationEvent = new NotificationEvent(
                        eventId,
                        account.getId(),
                        account.getEmail(),
                        String.format("Crédito de R$ %.2f realizado com sucesso. Novo saldo: R$ %.2f", 
                                     request.getAmount(), account.getBalance()),
                        NotificationType.TRANSACTION_SUCCESS
                    );
                    eventProducer.publishNotificationEvent(notificationEvent);
                }
            }
            
            if (metricsService != null && kafkaPublishSample != null) {
                metricsService.recordKafkaPublishTime(kafkaPublishSample);
            }
            
            logger.info("Crédito processado e eventos publicados para conta: {}", request.getAccountId());
            return response;
            
        } catch (Exception e) {
            if (metricsService != null && kafkaPublishSample != null) {
                metricsService.recordKafkaPublishTime(kafkaPublishSample);
            }
            logger.error("Erro ao processar crédito assíncrono: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    public TransactionResponse debitAsync(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "debitAsync");
        MDC.put("accountId", String.valueOf(request.getAccountId()));
        
        Timer.Sample kafkaPublishSample = null;
        if (metricsService != null) {
            kafkaPublishSample = metricsService.startKafkaPublishTimer();
        }
        
        try {
            // Executar transação síncrona
            TransactionResponse response = accountService.debit(request);
            
            // Publicar eventos assíncronos
            String eventId = UUID.randomUUID().toString();
            
            TransactionEvent transactionEvent = new TransactionEvent(
                eventId,
                request.getAccountId(),
                request.getAmount(),
                TransactionType.DEBIT,
                response.getStatus(),
                response.getMessage()
            );
            
            // Publicar evento de transação
            eventProducer.publishTransactionEvent(transactionEvent);
            
            // Publicar evento de auditoria
            eventProducer.publishAuditEvent(transactionEvent);
            
            // Publicar notificação
            Account account = accountService.getAccountById(request.getAccountId());
            if (account.getEmail() != null && !account.getEmail().isEmpty()) {
                if (response.getStatus() == Status.EFETUADO) {
                    NotificationEvent notificationEvent = new NotificationEvent(
                        eventId,
                        account.getId(),
                        account.getEmail(),
                        String.format("Débito de R$ %.2f realizado com sucesso. Novo saldo: R$ %.2f", 
                                     request.getAmount(), account.getBalance()),
                        NotificationType.TRANSACTION_SUCCESS
                    );
                    eventProducer.publishNotificationEvent(notificationEvent);
                } else {
                    NotificationEvent notificationEvent = new NotificationEvent(
                        eventId,
                        account.getId(),
                        account.getEmail(),
                        String.format("Débito de R$ %.2f falhou: %s", 
                                     request.getAmount(), response.getMessage()),
                        NotificationType.TRANSACTION_FAILED
                    );
                    eventProducer.publishNotificationEvent(notificationEvent);
                }
            }
            
            if (metricsService != null && kafkaPublishSample != null) {
                metricsService.recordKafkaPublishTime(kafkaPublishSample);
            }
            
            logger.info("Débito processado e eventos publicados para conta: {}", request.getAccountId());
            return response;
            
        } catch (Exception e) {
            if (metricsService != null && kafkaPublishSample != null) {
                metricsService.recordKafkaPublishTime(kafkaPublishSample);
            }
            logger.error("Erro ao processar débito assíncrono: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}