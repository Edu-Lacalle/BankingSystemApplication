package com.bank.BankingSystemApplication.service;

import com.bank.BankingSystemApplication.audit.BankingAuditService;
import com.bank.BankingSystemApplication.dto.*;
import com.bank.BankingSystemApplication.entity.Account;
import com.bank.BankingSystemApplication.metrics.BankingMetricsService;
import com.bank.BankingSystemApplication.service.kafka.TransactionEventProducer;
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
        MDC.put("cpf", request.getCpf().substring(0, 3) + "*****" + request.getCpf().substring(8));
        
        Timer.Sample kafkaPublishSample = metricsService.startKafkaPublishTimer();
        
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
            // Publicar evento de auditoria de forma assíncrona
            eventProducer.publishAuditEvent(auditEvent)
                    .exceptionally(throwable -> {
                        logger.error("Erro ao publicar evento de auditoria para conta {}: {}", 
                                   account.getId(), throwable.getMessage());
                        auditService.auditKafkaEvent("account-audit", "AUDIT_EVENT", eventId, 
                                                    false, "Erro: " + throwable.getMessage());
                        return null;
                    })
                    .thenRun(() -> {
                        auditService.auditKafkaEvent("account-audit", "AUDIT_EVENT", eventId, 
                                                    true, "Evento de auditoria publicado com sucesso");
                    });
            
            // Evento de notificação (se email estiver presente)
            if (account.getEmail() != null && !account.getEmail().isEmpty()) {
                NotificationEvent notificationEvent = new NotificationEvent(
                    eventId,
                    account.getId(),
                    account.getEmail(),
                    "Bem-vindo(a) ao nosso banco! Sua conta foi criada com sucesso.",
                    NotificationType.ACCOUNT_CREATED
                );
                // Publicar evento de notificação de forma assíncrona
                eventProducer.publishNotificationEvent(notificationEvent)
                        .exceptionally(throwable -> {
                            logger.error("Erro ao publicar evento de notificação para conta {}: {}", 
                                       account.getId(), throwable.getMessage());
                            auditService.auditKafkaEvent("notifications", "NOTIFICATION_EVENT", eventId, 
                                                        false, "Erro: " + throwable.getMessage());
                            return null;
                        })
                        .thenRun(() -> {
                            auditService.auditKafkaEvent("notifications", "NOTIFICATION_EVENT", eventId, 
                                                        true, "Evento de notificação publicado com sucesso");
                        });
            }
            
            metricsService.recordKafkaPublishTime(kafkaPublishSample);
            logger.info("Conta criada e eventos publicados para ID: {}", account.getId());
            return account;
            
        } catch (Exception e) {
            metricsService.recordKafkaPublishTime(kafkaPublishSample);
            auditService.auditSystemFailure("AsyncAccountService", "createAccountAsync", 
                                           e.getMessage(), e.getStackTrace().toString(), correlationId);
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
        
        Timer.Sample kafkaPublishSample = metricsService.startKafkaPublishTimer();
        
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
            eventProducer.publishTransactionEvent(transactionEvent)
                    .exceptionally(throwable -> {
                        auditService.auditKafkaEvent("transactions", "TRANSACTION_EVENT", eventId, 
                                                    false, "Erro: " + throwable.getMessage());
                        return null;
                    })
                    .thenRun(() -> {
                        auditService.auditKafkaEvent("transactions", "TRANSACTION_EVENT", eventId, 
                                                    true, "Evento de transação publicado com sucesso");
                    });
            
            // Publicar evento de auditoria
            eventProducer.publishAuditEvent(transactionEvent)
                    .exceptionally(throwable -> {
                        auditService.auditKafkaEvent("account-audit", "AUDIT_EVENT", eventId, 
                                                    false, "Erro: " + throwable.getMessage());
                        return null;
                    });
            
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
                    eventProducer.publishNotificationEvent(notificationEvent)
                            .exceptionally(throwable -> {
                                auditService.auditKafkaEvent("notifications", "NOTIFICATION_EVENT", eventId, 
                                                            false, "Erro: " + throwable.getMessage());
                                return null;
                            });
                }
            }
            
            metricsService.recordKafkaPublishTime(kafkaPublishSample);
            logger.info("Crédito processado e eventos publicados para conta: {}", request.getAccountId());
            return response;
            
        } catch (Exception e) {
            metricsService.recordKafkaPublishTime(kafkaPublishSample);
            auditService.auditSystemFailure("AsyncAccountService", "creditAsync", 
                                           e.getMessage(), e.getStackTrace().toString(), correlationId);
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
        
        Timer.Sample kafkaPublishSample = metricsService.startKafkaPublishTimer();
        
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
            eventProducer.publishTransactionEvent(transactionEvent)
                    .exceptionally(throwable -> {
                        auditService.auditKafkaEvent("transactions", "TRANSACTION_EVENT", eventId, 
                                                    false, "Erro: " + throwable.getMessage());
                        return null;
                    })
                    .thenRun(() -> {
                        auditService.auditKafkaEvent("transactions", "TRANSACTION_EVENT", eventId, 
                                                    true, "Evento de transação publicado com sucesso");
                    });
            
            // Publicar evento de auditoria
            eventProducer.publishAuditEvent(transactionEvent)
                    .exceptionally(throwable -> {
                        auditService.auditKafkaEvent("account-audit", "AUDIT_EVENT", eventId, 
                                                    false, "Erro: " + throwable.getMessage());
                        return null;
                    });
            
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
                    eventProducer.publishNotificationEvent(notificationEvent)
                            .exceptionally(throwable -> {
                                auditService.auditKafkaEvent("notifications", "NOTIFICATION_EVENT", eventId, 
                                                            false, "Erro: " + throwable.getMessage());
                                return null;
                            });
                } else {
                    NotificationEvent notificationEvent = new NotificationEvent(
                        eventId,
                        account.getId(),
                        account.getEmail(),
                        String.format("Débito de R$ %.2f falhou: %s", 
                                     request.getAmount(), response.getMessage()),
                        NotificationType.TRANSACTION_FAILED
                    );
                    eventProducer.publishNotificationEvent(notificationEvent)
                            .exceptionally(throwable -> {
                                auditService.auditKafkaEvent("notifications", "NOTIFICATION_EVENT", eventId, 
                                                            false, "Erro: " + throwable.getMessage());
                                return null;
                            });
                }
            }
            
            metricsService.recordKafkaPublishTime(kafkaPublishSample);
            logger.info("Débito processado e eventos publicados para conta: {}", request.getAccountId());
            return response;
            
        } catch (Exception e) {
            metricsService.recordKafkaPublishTime(kafkaPublishSample);
            auditService.auditSystemFailure("AsyncAccountService", "debitAsync", 
                                           e.getMessage(), e.getStackTrace().toString(), correlationId);
            logger.error("Erro ao processar débito assíncrono: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}