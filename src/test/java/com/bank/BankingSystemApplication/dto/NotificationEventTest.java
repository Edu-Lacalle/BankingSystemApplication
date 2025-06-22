package com.bank.BankingSystemApplication.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEventTest {

    private NotificationEvent notificationEvent;

    @BeforeEach
    void setUp() {
        notificationEvent = new NotificationEvent();
    }

    @Test
    void testDefaultConstructor() {
        // Act
        NotificationEvent event = new NotificationEvent();

        // Assert
        assertNull(event.getEventId());
        assertNull(event.getAccountId());
        assertNull(event.getEmail());
        assertNull(event.getMessage());
        assertNull(event.getType());
        assertNull(event.getTimestamp());
    }

    @Test
    void testParameterizedConstructor() {
        // Arrange
        String eventId = "notification-123";
        Long accountId = 1L;
        String email = "cliente@email.com";
        String message = "Bem-vindo ao nosso banco!";
        NotificationType type = NotificationType.ACCOUNT_CREATED;

        // Act
        NotificationEvent event = new NotificationEvent(eventId, accountId, email, message, type);

        // Assert
        assertEquals(eventId, event.getEventId());
        assertEquals(accountId, event.getAccountId());
        assertEquals(email, event.getEmail());
        assertEquals(message, event.getMessage());
        assertEquals(type, event.getType());
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        String eventId = "notification-456";
        Long accountId = 2L;
        String email = "usuario@banco.com";
        String message = "Transação realizada com sucesso";
        NotificationType type = NotificationType.TRANSACTION_SUCCESS;
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationEvent.setEventId(eventId);
        notificationEvent.setAccountId(accountId);
        notificationEvent.setEmail(email);
        notificationEvent.setMessage(message);
        notificationEvent.setType(type);
        notificationEvent.setTimestamp(timestamp);

        // Assert
        assertEquals(eventId, notificationEvent.getEventId());
        assertEquals(accountId, notificationEvent.getAccountId());
        assertEquals(email, notificationEvent.getEmail());
        assertEquals(message, notificationEvent.getMessage());
        assertEquals(type, notificationEvent.getType());
        assertEquals(timestamp, notificationEvent.getTimestamp());
    }

    @Test
    void testWithNullValues() {
        // Act
        notificationEvent.setEventId(null);
        notificationEvent.setAccountId(null);
        notificationEvent.setEmail(null);
        notificationEvent.setMessage(null);
        notificationEvent.setType(null);
        notificationEvent.setTimestamp(null);

        // Assert
        assertNull(notificationEvent.getEventId());
        assertNull(notificationEvent.getAccountId());
        assertNull(notificationEvent.getEmail());
        assertNull(notificationEvent.getMessage());
        assertNull(notificationEvent.getType());
        assertNull(notificationEvent.getTimestamp());
    }

    @Test
    void testWithAllNotificationTypes() {
        // Test with all enum values
        for (NotificationType type : NotificationType.values()) {
            notificationEvent.setType(type);
            assertEquals(type, notificationEvent.getType());
        }
    }

    @Test
    void testTimestampAutomaticSetting() {
        // Arrange
        LocalDateTime before = LocalDateTime.now();

        // Act
        NotificationEvent event = new NotificationEvent(
                "notification-123", 1L, "test@email.com", 
                "Test message", NotificationType.ACCOUNT_CREATED
        );

        // Assert
        LocalDateTime after = LocalDateTime.now();
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(event.getTimestamp().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testWithEmptyStrings() {
        // Act
        notificationEvent.setEventId("");
        notificationEvent.setEmail("");
        notificationEvent.setMessage("");

        // Assert
        assertEquals("", notificationEvent.getEventId());
        assertEquals("", notificationEvent.getEmail());
        assertEquals("", notificationEvent.getMessage());
    }

    @Test
    void testWithLongMessage() {
        // Arrange
        String longMessage = "This is a very long notification message that could potentially be sent to a customer. ".repeat(10);

        // Act
        notificationEvent.setMessage(longMessage);

        // Assert
        assertEquals(longMessage, notificationEvent.getMessage());
    }

    @Test
    void testWithInvalidEmailFormat() {
        // Arrange
        String invalidEmail = "invalid-email-format";

        // Act
        notificationEvent.setEmail(invalidEmail);

        // Assert
        assertEquals(invalidEmail, notificationEvent.getEmail());
        // Note: Email validation should be handled at the service/validation layer, not in the DTO
    }
}