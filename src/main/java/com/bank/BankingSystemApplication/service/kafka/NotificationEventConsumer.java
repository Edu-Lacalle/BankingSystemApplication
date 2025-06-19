package com.bank.BankingSystemApplication.service.kafka;

import com.bank.BankingSystemApplication.config.KafkaConfig;
import com.bank.BankingSystemApplication.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);
    
    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC, groupId = "banking-notification-group")
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            logger.info("Processando notificação: {} para conta: {}", 
                       event.getEventId(), event.getAccountId());
            
            // Aqui implementaríamos diferentes tipos de notificação:
            switch (event.getType()) {
                case ACCOUNT_CREATED:
                    sendWelcomeEmail(event);
                    break;
                case TRANSACTION_SUCCESS:
                    sendTransactionConfirmation(event);
                    break;
                case TRANSACTION_FAILED:
                    sendTransactionFailure(event);
                    break;
                case BALANCE_LOW:
                    sendBalanceAlert(event);
                    break;
            }
            
            logger.info("Notificação processada com sucesso: {}", event.getEventId());
            
        } catch (Exception e) {
            logger.error("Erro ao processar notificação {}: {}", 
                        event.getEventId(), e.getMessage(), e);
        }
    }
    
    private void sendWelcomeEmail(NotificationEvent event) {
        logger.info("Enviando email de boas-vindas para: {}", event.getEmail());
        // Implementação do envio de email de boas-vindas
    }
    
    private void sendTransactionConfirmation(NotificationEvent event) {
        logger.info("Enviando confirmação de transação para: {}", event.getEmail());
        // Implementação do envio de confirmação de transação
    }
    
    private void sendTransactionFailure(NotificationEvent event) {
        logger.info("Enviando notificação de falha na transação para: {}", event.getEmail());
        // Implementação do envio de notificação de falha
    }
    
    private void sendBalanceAlert(NotificationEvent event) {
        logger.info("Enviando alerta de saldo baixo para: {}", event.getEmail());
        // Implementação do envio de alerta de saldo
    }
}