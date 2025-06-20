package com.bank.BankingSystemApplication.cqrs.command;

import com.bank.BankingSystemApplication.dto.TransactionRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command base para operações de transação seguindo o padrão CQRS
 */
public abstract class TransactionCommand {
    
    private final String commandId;
    private final LocalDateTime timestamp;
    private final Long accountId;
    private final BigDecimal amount;
    private final String correlationId;
    private final TransactionType type;
    
    public enum TransactionType {
        CREDIT, DEBIT
    }
    
    protected TransactionCommand(TransactionRequest request, TransactionType type) {
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.accountId = request.getAccountId();
        this.amount = request.getAmount();
        this.correlationId = UUID.randomUUID().toString();
        this.type = type;
    }
    
    protected TransactionCommand(TransactionRequest request, TransactionType type, String correlationId) {
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.accountId = request.getAccountId();
        this.amount = request.getAmount();
        this.correlationId = correlationId;
        this.type = type;
    }
    
    public String getCommandId() {
        return commandId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "TransactionCommand{" +
                "commandId='" + commandId + '\'' +
                ", timestamp=" + timestamp +
                ", accountId=" + accountId +
                ", amount=" + amount +
                ", type=" + type +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}