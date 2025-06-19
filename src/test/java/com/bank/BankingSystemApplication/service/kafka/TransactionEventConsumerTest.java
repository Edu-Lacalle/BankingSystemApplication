package com.bank.BankingSystemApplication.service.kafka;

import com.bank.BankingSystemApplication.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @InjectMocks
    private TransactionEventConsumer transactionEventConsumer;

    private TransactionEvent transactionEvent;

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
    }

    @Test
    void testHandleTransactionEventSuccess() {
        // Act - Should not throw any exception
        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleTransactionEvent(transactionEvent);
        });
    }

    @Test
    void testHandleAuditEventSuccess() {
        // Act - Should not throw any exception
        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleAuditEvent(transactionEvent);
        });
    }

    @Test
    void testHandleTransactionEventWithNullEvent() {
        // Act - Should handle gracefully even with null event
        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleTransactionEvent(null);
        });
    }

    @Test
    void testHandleAuditEventWithNullEvent() {
        // Act - Should handle gracefully even with null event
        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleAuditEvent(null);
        });
    }

    @Test
    void testHandleTransactionEventWithDifferentTypes() {
        // Test with DEBIT transaction
        TransactionEvent debitEvent = new TransactionEvent(
                "debit-123",
                2L,
                new BigDecimal("50.00"),
                TransactionType.DEBIT,
                Status.EFETUADO,
                "Débito efetuado com sucesso"
        );

        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleTransactionEvent(debitEvent);
        });

        // Test with ACCOUNT_CREATION transaction
        TransactionEvent accountCreationEvent = new TransactionEvent(
                "account-123",
                3L,
                BigDecimal.ZERO,
                TransactionType.ACCOUNT_CREATION,
                Status.EFETUADO,
                "Conta criada com sucesso"
        );

        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleTransactionEvent(accountCreationEvent);
        });
    }

    @Test
    void testHandleTransactionEventWithFailedStatus() {
        // Arrange
        TransactionEvent failedEvent = new TransactionEvent(
                "failed-123",
                1L,
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                Status.RECUSADO,
                "Conta não encontrada"
        );

        // Act - Should handle failed transactions gracefully
        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleTransactionEvent(failedEvent);
        });
    }

    @Test
    void testHandleEventWithMissingFields() {
        // Arrange - Event with null fields
        TransactionEvent incompleteEvent = new TransactionEvent();
        incompleteEvent.setEventId("incomplete-123");
        // Other fields are null

        // Act - Should handle incomplete events gracefully
        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleTransactionEvent(incompleteEvent);
            transactionEventConsumer.handleAuditEvent(incompleteEvent);
        });
    }
}