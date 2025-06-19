package com.bank.BankingSystemApplication.service.kafka;

import com.bank.BankingSystemApplication.config.KafkaConfig;
import com.bank.BankingSystemApplication.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionEventProducer transactionEventProducer;

    private TransactionEvent transactionEvent;
    private NotificationEvent notificationEvent;

    @BeforeEach
    void setUp() {
        transactionEvent = new TransactionEvent(
                "event-123",
                1L,
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                Status.EFETUADO,
                "Crédito efetuado com sucesso"
        );

        notificationEvent = new NotificationEvent(
                "notification-123",
                1L,
                "cliente@email.com",
                "Transação realizada com sucesso",
                NotificationType.TRANSACTION_SUCCESS
        );
    }

    @Test
    void testPublishTransactionEventSuccess() {
        // Act
        transactionEventProducer.publishTransactionEvent(transactionEvent);

        // Assert
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.TRANSACTION_TOPIC),
                eq("event-123"),
                eq(transactionEvent)
        );
    }

    @Test
    void testPublishNotificationEventSuccess() {
        // Act
        transactionEventProducer.publishNotificationEvent(notificationEvent);

        // Assert
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.NOTIFICATION_TOPIC),
                eq("notification-123"),
                eq(notificationEvent)
        );
    }

    @Test
    void testPublishAuditEventSuccess() {
        // Act
        transactionEventProducer.publishAuditEvent(transactionEvent);

        // Assert
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.AUDIT_TOPIC),
                eq("event-123"),
                eq(transactionEvent)
        );
    }

    @Test
    void testPublishTransactionEventWithException() {
        // Arrange
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka connection error"));

        // Act - Should not throw exception (error is logged)
        transactionEventProducer.publishTransactionEvent(transactionEvent);

        // Assert
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.TRANSACTION_TOPIC),
                eq("event-123"),
                eq(transactionEvent)
        );
    }

    @Test
    void testPublishNotificationEventWithException() {
        // Arrange
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka connection error"));

        // Act - Should not throw exception (error is logged)
        transactionEventProducer.publishNotificationEvent(notificationEvent);

        // Assert
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.NOTIFICATION_TOPIC),
                eq("notification-123"),
                eq(notificationEvent)
        );
    }

    @Test
    void testPublishAuditEventWithException() {
        // Arrange
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka connection error"));

        // Act - Should not throw exception (error is logged)
        transactionEventProducer.publishAuditEvent(transactionEvent);

        // Assert
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.AUDIT_TOPIC),
                eq("event-123"),
                eq(transactionEvent)
        );
    }

    @Test
    void testPublishEventsWithNullEventId() {
        // Arrange
        transactionEvent.setEventId(null);
        notificationEvent.setEventId(null);

        // Act
        transactionEventProducer.publishTransactionEvent(transactionEvent);
        transactionEventProducer.publishNotificationEvent(notificationEvent);
        transactionEventProducer.publishAuditEvent(transactionEvent);

        // Assert - Should still attempt to send even with null eventId
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.TRANSACTION_TOPIC),
                eq(null),
                eq(transactionEvent)
        );
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.NOTIFICATION_TOPIC),
                eq(null),
                eq(notificationEvent)
        );
        verify(kafkaTemplate, times(1)).send(
                eq(KafkaConfig.AUDIT_TOPIC),
                eq(null),
                eq(transactionEvent)
        );
    }
}