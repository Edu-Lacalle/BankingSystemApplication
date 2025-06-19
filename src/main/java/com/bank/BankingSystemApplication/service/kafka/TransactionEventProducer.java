package com.bank.BankingSystemApplication.service.kafka;

import com.bank.BankingSystemApplication.config.KafkaConfig;
import com.bank.BankingSystemApplication.dto.TransactionEvent;
import com.bank.BankingSystemApplication.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionEventProducer.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishTransactionEvent(TransactionEvent event) {
        try {
            logger.info("Publicando evento de transação: {}", event.getEventId());
            kafkaTemplate.send(KafkaConfig.TRANSACTION_TOPIC, event.getEventId(), event);
        } catch (Exception e) {
            logger.error("Erro ao publicar evento de transação: {}", e.getMessage(), e);
        }
    }
    
    public void publishNotificationEvent(NotificationEvent event) {
        try {
            logger.info("Publicando evento de notificação: {}", event.getEventId());
            kafkaTemplate.send(KafkaConfig.NOTIFICATION_TOPIC, event.getEventId(), event);
        } catch (Exception e) {
            logger.error("Erro ao publicar evento de notificação: {}", e.getMessage(), e);
        }
    }
    
    public void publishAuditEvent(TransactionEvent event) {
        try {
            logger.info("Publicando evento de auditoria: {}", event.getEventId());
            kafkaTemplate.send(KafkaConfig.AUDIT_TOPIC, event.getEventId(), event);
        } catch (Exception e) {
            logger.error("Erro ao publicar evento de auditoria: {}", e.getMessage(), e);
        }
    }
}