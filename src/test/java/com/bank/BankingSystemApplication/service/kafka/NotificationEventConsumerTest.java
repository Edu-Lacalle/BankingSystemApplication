package com.bank.BankingSystemApplication.service.kafka;

import com.bank.BankingSystemApplication.dto.NotificationEvent;
import com.bank.BankingSystemApplication.dto.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    private NotificationEvent notificationEvent;

    @BeforeEach
    void setUp() {
        notificationEvent = new NotificationEvent(
                "notification-123",
                1L,
                "cliente@email.com",
                "Bem-vindo ao nosso banco!",
                NotificationType.ACCOUNT_CREATED
        );
    }

    @Test
    void testHandleNotificationEventAccountCreated() {
        // Arrange
        notificationEvent.setType(NotificationType.ACCOUNT_CREATED);

        // Act - Should not throw any exception
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(notificationEvent);
        });
    }

    @Test
    void testHandleNotificationEventTransactionSuccess() {
        // Arrange
        notificationEvent.setType(NotificationType.TRANSACTION_SUCCESS);
        notificationEvent.setMessage("Transação realizada com sucesso");

        // Act - Should not throw any exception
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(notificationEvent);
        });
    }

    @Test
    void testHandleNotificationEventTransactionFailed() {
        // Arrange
        notificationEvent.setType(NotificationType.TRANSACTION_FAILED);
        notificationEvent.setMessage("Transação falhou: saldo insuficiente");

        // Act - Should not throw any exception
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(notificationEvent);
        });
    }

    @Test
    void testHandleNotificationEventBalanceLow() {
        // Arrange
        notificationEvent.setType(NotificationType.BALANCE_LOW);
        notificationEvent.setMessage("Seu saldo está baixo");

        // Act - Should not throw any exception
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(notificationEvent);
        });
    }

    @Test
    void testHandleNotificationEventWithNullEvent() {
        // Act - Should handle gracefully even with null event
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(null);
        });
    }

    @Test
    void testHandleNotificationEventWithNullType() {
        // Arrange
        notificationEvent.setType(null);

        // Act - Should handle gracefully even with null type
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(notificationEvent);
        });
    }

    @Test
    void testHandleNotificationEventWithMissingFields() {
        // Arrange - Event with null fields
        NotificationEvent incompleteEvent = new NotificationEvent();
        incompleteEvent.setEventId("incomplete-123");
        // Other fields are null

        // Act - Should handle incomplete events gracefully
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(incompleteEvent);
        });
    }

    @Test
    void testHandleNotificationEventWithAllTypes() {
        // Test all notification types
        for (NotificationType type : NotificationType.values()) {
            NotificationEvent event = new NotificationEvent(
                    "test-" + type.name(),
                    1L,
                    "test@email.com",
                    "Test message for " + type.name(),
                    type
            );

            assertDoesNotThrow(() -> {
                notificationEventConsumer.handleNotificationEvent(event);
            });
        }
    }

    @Test
    void testHandleNotificationEventWithInvalidEmail() {
        // Arrange
        notificationEvent.setEmail("invalid-email");

        // Act - Should handle invalid email gracefully
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(notificationEvent);
        });
    }

    @Test
    void testHandleNotificationEventWithEmptyMessage() {
        // Arrange
        notificationEvent.setMessage("");

        // Act - Should handle empty message gracefully
        assertDoesNotThrow(() -> {
            notificationEventConsumer.handleNotificationEvent(notificationEvent);
        });
    }
}