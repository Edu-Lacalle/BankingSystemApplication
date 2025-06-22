package com.bank.BankingSystemApplication.adapter.out.messaging;

import com.bank.BankingSystemApplication.domain.port.out.EventPublishingPort;
import com.bank.BankingSystemApplication.domain.model.TransactionEvent;
import com.bank.BankingSystemApplication.domain.model.NotificationEvent;
import com.bank.BankingSystemApplication.application.service.kafka.TransactionEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublishingAdapter implements EventPublishingPort {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublishingAdapter.class);
    
    @Autowired
    private TransactionEventProducer transactionEventProducer;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publishTransactionEvent(TransactionEvent event) {
        logger.info("Adapter: Publishing transaction event for account: {}", event.getAccountId());
        transactionEventProducer.sendTransactionEvent(event);
    }
    
    @Override
    public void publishNotificationEvent(NotificationEvent event) {
        logger.info("Adapter: Publishing notification event for account: {}", event.getAccountId());
        kafkaTemplate.send("banking.notifications", event);
    }
}