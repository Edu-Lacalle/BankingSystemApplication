package com.bank.BankingSystemApplication.infrastructure.async;

import com.bank.BankingSystemApplication.domain.port.out.EventPublishingPort;
import com.bank.BankingSystemApplication.domain.model.NotificationEvent;
import com.bank.BankingSystemApplication.domain.model.NotificationType;
import com.bank.BankingSystemApplication.domain.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AsyncNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncNotificationService.class);
    
    @Autowired
    private EventPublishingPort eventPort;
    
    @Async
    public void publishAccountCreationNotification(Account account) {
        try {
            NotificationEvent notificationEvent = new NotificationEvent();
            notificationEvent.setAccountId(account.getId());
            notificationEvent.setMessage("Account created successfully");
            notificationEvent.setType(NotificationType.ACCOUNT_CREATED);
            notificationEvent.setTimestamp(LocalDateTime.now());
            
            // Publish asynchronously to avoid blocking the main transaction
            eventPort.publishNotificationEvent(notificationEvent);
            
            logger.debug("Notification event published asynchronously for account: {}", account.getId());
        } catch (Exception e) {
            logger.error("Failed to publish notification event for account: {}", account.getId(), e);
        }
    }
}