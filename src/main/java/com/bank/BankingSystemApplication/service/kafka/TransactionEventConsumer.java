package com.bank.BankingSystemApplication.service.kafka;

import com.bank.BankingSystemApplication.config.KafkaConfig;
import com.bank.BankingSystemApplication.dto.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionEventConsumer.class);
    
    @KafkaListener(topics = KafkaConfig.TRANSACTION_TOPIC, groupId = "banking-transaction-group")
    public void handleTransactionEvent(TransactionEvent event) {
        if (event == null) {
            logger.warn("Evento de transação nulo recebido, ignorando");
            return;
        }
        
        try {
            logger.info("Processando evento de transação: {} para conta: {}", 
                       event.getEventId(), event.getAccountId());
            
            // Aqui podemos implementar lógicas assíncronas como:
            // - Validações adicionais
            // - Integração com sistemas externos
            // - Cálculo de taxas
            // - Análise de fraude
            
            logger.info("Evento de transação processado com sucesso: {}", event.getEventId());
            
        } catch (Exception e) {
            logger.error("Erro ao processar evento de transação {}: {}", 
                        event.getEventId(), e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = KafkaConfig.AUDIT_TOPIC, groupId = "banking-audit-group")
    public void handleAuditEvent(TransactionEvent event) {
        if (event == null) {
            logger.warn("Evento de auditoria nulo recebido, ignorando");
            return;
        }
        
        try {
            logger.info("Registrando auditoria para evento: {} - Conta: {} - Valor: {}", 
                       event.getEventId(), event.getAccountId(), event.getAmount());
            
            // Aqui implementaríamos:
            // - Log detalhado para auditoria
            // - Armazenamento em sistema de auditoria
            // - Compliance e relatórios
            
            logger.info("Auditoria registrada para evento: {}", event.getEventId());
            
        } catch (Exception e) {
            logger.error("Erro ao processar auditoria do evento {}: {}", 
                        event.getEventId(), e.getMessage(), e);
        }
    }
}