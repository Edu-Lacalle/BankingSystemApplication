package com.bank.BankingSystemApplication.domain.model;

import java.time.LocalDateTime;

public class NotificationEvent {
    
    private String eventId;
    private Long accountId;
    private String email;
    private String message;
    private NotificationType type;
    private LocalDateTime timestamp;
    
    public NotificationEvent() {}
    
    public NotificationEvent(String eventId, Long accountId, String email, 
                           String message, NotificationType type) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.email = email;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}