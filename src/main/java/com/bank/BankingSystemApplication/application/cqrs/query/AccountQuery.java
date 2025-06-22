package com.bank.BankingSystemApplication.application.cqrs.query;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Query para consultas de conta seguindo o padrão CQRS
 */
public class AccountQuery {
    
    private final String queryId;
    private final LocalDateTime timestamp;
    private final Long accountId;
    private final String correlationId;
    
    public AccountQuery(Long accountId) {
        this.queryId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.accountId = accountId;
        this.correlationId = UUID.randomUUID().toString();
    }
    
    public AccountQuery(Long accountId, String correlationId) {
        this.queryId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.accountId = accountId;
        this.correlationId = correlationId;
    }
    
    public String getQueryId() {
        return queryId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public String toString() {
        return "AccountQuery{" +
                "queryId='" + queryId + '\'' +
                ", timestamp=" + timestamp +
                ", accountId=" + accountId +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}