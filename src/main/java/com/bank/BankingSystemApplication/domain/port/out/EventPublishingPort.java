package com.bank.BankingSystemApplication.domain.port.out;

import com.bank.BankingSystemApplication.domain.model.TransactionEvent;
import com.bank.BankingSystemApplication.domain.model.NotificationEvent;

public interface EventPublishingPort {
    void publishTransactionEvent(TransactionEvent event);
    void publishNotificationEvent(NotificationEvent event);
}