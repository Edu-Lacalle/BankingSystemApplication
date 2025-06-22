package com.bank.BankingSystemApplication.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionEvent {
    
    private String eventId;
    private Long accountId;
    private BigDecimal amount;
    private TransactionType type;
    private Status status;
    private String message;
    private LocalDateTime timestamp;
    private boolean success;
    private BigDecimal balance;
    
    public TransactionEvent() {}
    
    public TransactionEvent(String eventId, Long accountId, BigDecimal amount, 
                           TransactionType type, Status status, String message) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.message = message;
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
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public void setType(TransactionType type) {
        this.type = type;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}