package com.bank.BankingSystemApplication.cqrs.command;

import com.bank.BankingSystemApplication.dto.AccountCreationRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command para criação de conta seguindo o padrão CQRS
 */
public class CreateAccountCommand {
    
    private final String commandId;
    private final LocalDateTime timestamp;
    private final AccountCreationRequest accountRequest;
    private final String correlationId;
    
    public CreateAccountCommand(AccountCreationRequest accountRequest) {
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.accountRequest = accountRequest;
        this.correlationId = UUID.randomUUID().toString();
    }
    
    public CreateAccountCommand(AccountCreationRequest accountRequest, String correlationId) {
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.accountRequest = accountRequest;
        this.correlationId = correlationId;
    }
    
    public String getCommandId() {
        return commandId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public AccountCreationRequest getAccountRequest() {
        return accountRequest;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public String toString() {
        return "CreateAccountCommand{" +
                "commandId='" + commandId + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                ", cpf='" + accountRequest.getCpf() + '\'' +
                '}';
    }
}