package com.bank.BankingSystemApplication.application.service.kafka;

import com.bank.BankingSystemApplication.infrastructure.config.KafkaConfig;
import com.bank.BankingSystemApplication.domain.model.TransactionEvent;
import com.bank.BankingSystemApplication.domain.model.NotificationEvent;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TransactionEventProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionEventProducer.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Retry(name = "banking-service", fallbackMethod = "fallbackPublishTransactionEvent")
    @CircuitBreaker(name = "banking-service")
    public CompletableFuture<SendResult<String, Object>> publishTransactionEvent(TransactionEvent event) {
        try {
            logger.info("Publicando evento de transação com retry: {}", event.getEventId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(KafkaConfig.TRANSACTION_TOPIC, event.getEventId(), event);
            if (future == null) {
                logger.warn("KafkaTemplate.send retornou null para evento: {}", event.getEventId());
                return CompletableFuture.completedFuture(null);
            }
            
            return future.whenComplete((result, exception) -> {
                if (exception == null) {
                    if (result != null) {
                        logger.info("Evento de transação publicado com sucesso: {} no offset: {}", 
                                   event.getEventId(), result.getRecordMetadata().offset());
                    } else {
                        logger.info("Evento de transação publicado: {}", event.getEventId());
                    }
                } else {
                    logger.error("Falha ao publicar evento de transação: {}", exception.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Erro ao publicar evento de transação: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    @Retry(name = "banking-service", fallbackMethod = "fallbackPublishNotificationEvent")
    @CircuitBreaker(name = "banking-service")
    public CompletableFuture<SendResult<String, Object>> publishNotificationEvent(NotificationEvent event) {
        try {
            logger.info("Publicando evento de notificação com retry: {}", event.getEventId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(KafkaConfig.NOTIFICATION_TOPIC, event.getEventId(), event);
            if (future == null) {
                logger.warn("KafkaTemplate.send retornou null para evento: {}", event.getEventId());
                return CompletableFuture.completedFuture(null);
            }
            
            return future.whenComplete((result, exception) -> {
                if (exception == null) {
                    if (result != null) {
                        logger.info("Evento de notificação publicado com sucesso: {} no offset: {}", 
                                   event.getEventId(), result.getRecordMetadata().offset());
                    } else {
                        logger.info("Evento de notificação publicado: {}", event.getEventId());
                    }
                } else {
                    logger.error("Falha ao publicar evento de notificação: {}", exception.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Erro ao publicar evento de notificação: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    @Retry(name = "banking-service", fallbackMethod = "fallbackPublishAuditEvent")
    @CircuitBreaker(name = "banking-service")
    public CompletableFuture<SendResult<String, Object>> publishAuditEvent(TransactionEvent event) {
        try {
            logger.info("Publicando evento de auditoria com retry: {}", event.getEventId());
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(KafkaConfig.AUDIT_TOPIC, event.getEventId(), event);
            if (future == null) {
                logger.warn("KafkaTemplate.send retornou null para evento: {}", event.getEventId());
                return CompletableFuture.completedFuture(null);
            }
            
            return future.whenComplete((result, exception) -> {
                if (exception == null) {
                    if (result != null) {
                        logger.info("Evento de auditoria publicado com sucesso: {} no offset: {}", 
                                   event.getEventId(), result.getRecordMetadata().offset());
                    } else {
                        logger.info("Evento de auditoria publicado: {}", event.getEventId());
                    }
                } else {
                    logger.error("Falha ao publicar evento de auditoria: {}", exception.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Erro ao publicar evento de auditoria: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // Métodos de fallback
    public CompletableFuture<SendResult<String, Object>> fallbackPublishTransactionEvent(TransactionEvent event, Exception ex) {
        logger.error("Fallback ativado para evento de transação: {} - Erro: {}", event.getEventId(), ex.getMessage());
        // Em um cenário real, poderia tentar um mecanismo alternativo ou armazenar em DLQ
        return CompletableFuture.failedFuture(new RuntimeException("Falha ao publicar evento após múltiplas tentativas"));
    }
    
    public CompletableFuture<SendResult<String, Object>> fallbackPublishNotificationEvent(NotificationEvent event, Exception ex) {
        logger.error("Fallback ativado para evento de notificação: {} - Erro: {}", event.getEventId(), ex.getMessage());
        return CompletableFuture.failedFuture(new RuntimeException("Falha ao publicar evento após múltiplas tentativas"));
    }
    
    public CompletableFuture<SendResult<String, Object>> fallbackPublishAuditEvent(TransactionEvent event, Exception ex) {
        logger.error("Fallback ativado para evento de auditoria: {} - Erro: {}", event.getEventId(), ex.getMessage());
        return CompletableFuture.failedFuture(new RuntimeException("Falha ao publicar evento após múltiplas tentativas"));
    }
    
    // Method for compatibility with EventPublishingAdapter
    public void sendTransactionEvent(TransactionEvent event) {
        publishTransactionEvent(event);
    }
}