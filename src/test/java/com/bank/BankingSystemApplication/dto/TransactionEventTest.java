package com.bank.BankingSystemApplication.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionEventTest {

    private TransactionEvent transactionEvent;

    @BeforeEach
    void setUp() {
        transactionEvent = new TransactionEvent();
    }

    @Test
    void testDefaultConstructor() {
        // Act
        TransactionEvent event = new TransactionEvent();

        // Assert
        assertNull(event.getEventId());
        assertNull(event.getAccountId());
        assertNull(event.getAmount());
        assertNull(event.getType());
        assertNull(event.getStatus());
        assertNull(event.getMessage());
        assertNull(event.getTimestamp());
    }

    @Test
    void testParameterizedConstructor() {
        // Arrange
        String eventId = "event-123";
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        TransactionType type = TransactionType.CREDIT;
        Status status = Status.EFETUADO;
        String message = "Crédito efetuado com sucesso";

        // Act
        TransactionEvent event = new TransactionEvent(eventId, accountId, amount, type, status, message);

        // Assert
        assertEquals(eventId, event.getEventId());
        assertEquals(accountId, event.getAccountId());
        assertEquals(amount, event.getAmount());
        assertEquals(type, event.getType());
        assertEquals(status, event.getStatus());
        assertEquals(message, event.getMessage());
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        String eventId = "event-456";
        Long accountId = 2L;
        BigDecimal amount = new BigDecimal("250.75");
        TransactionType type = TransactionType.DEBIT;
        Status status = Status.RECUSADO;
        String message = "Saldo insuficiente";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        transactionEvent.setEventId(eventId);
        transactionEvent.setAccountId(accountId);
        transactionEvent.setAmount(amount);
        transactionEvent.setType(type);
        transactionEvent.setStatus(status);
        transactionEvent.setMessage(message);
        transactionEvent.setTimestamp(timestamp);

        // Assert
        assertEquals(eventId, transactionEvent.getEventId());
        assertEquals(accountId, transactionEvent.getAccountId());
        assertEquals(amount, transactionEvent.getAmount());
        assertEquals(type, transactionEvent.getType());
        assertEquals(status, transactionEvent.getStatus());
        assertEquals(message, transactionEvent.getMessage());
        assertEquals(timestamp, transactionEvent.getTimestamp());
    }

    @Test
    void testWithNullValues() {
        // Act
        transactionEvent.setEventId(null);
        transactionEvent.setAccountId(null);
        transactionEvent.setAmount(null);
        transactionEvent.setType(null);
        transactionEvent.setStatus(null);
        transactionEvent.setMessage(null);
        transactionEvent.setTimestamp(null);

        // Assert
        assertNull(transactionEvent.getEventId());
        assertNull(transactionEvent.getAccountId());
        assertNull(transactionEvent.getAmount());
        assertNull(transactionEvent.getType());
        assertNull(transactionEvent.getStatus());
        assertNull(transactionEvent.getMessage());
        assertNull(transactionEvent.getTimestamp());
    }

    @Test
    void testWithAllTransactionTypes() {
        // Test with all enum values
        for (TransactionType type : TransactionType.values()) {
            transactionEvent.setType(type);
            assertEquals(type, transactionEvent.getType());
        }
    }

    @Test
    void testWithAllStatusTypes() {
        // Test with all enum values
        for (Status status : Status.values()) {
            transactionEvent.setStatus(status);
            assertEquals(status, transactionEvent.getStatus());
        }
    }

    @Test
    void testTimestampAutomaticSetting() {
        // Arrange
        LocalDateTime before = LocalDateTime.now();

        // Act
        TransactionEvent event = new TransactionEvent(
                "event-123", 1L, BigDecimal.TEN, 
                TransactionType.CREDIT, Status.EFETUADO, "Test"
        );

        // Assert
        LocalDateTime after = LocalDateTime.now();
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(event.getTimestamp().isBefore(after.plusSeconds(1)));
    }
}