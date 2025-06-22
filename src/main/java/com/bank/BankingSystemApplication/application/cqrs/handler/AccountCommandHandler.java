package com.bank.BankingSystemApplication.application.cqrs.handler;

import com.bank.BankingSystemApplication.application.cqrs.command.CreateAccountCommand;
import com.bank.BankingSystemApplication.application.cqrs.command.CreditCommand;
import com.bank.BankingSystemApplication.application.cqrs.command.DebitCommand;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.application.service.ResilientAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Handler para comandos de conta seguindo o padrão CQRS
 * Responsável apenas por operações de escrita (Command)
 */
@Component
public class AccountCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountCommandHandler.class);
    
    @Autowired
    private ResilientAccountService resilientAccountService;
    
    /**
     * Processa comando de criação de conta
     */
    public CompletableFuture<Account> handle(CreateAccountCommand command) {
        logger.info("Processando comando de criação de conta: {}", command.getCommandId());
        
        try {
            return resilientAccountService.createAccountResilient(command.getAccountRequest())
                    .whenComplete((account, exception) -> {
                        if (exception == null) {
                            logger.info("Comando de criação de conta processado com sucesso: {} para conta: {}", 
                                       command.getCommandId(), account.getId());
                        } else {
                            logger.error("Falha ao processar comando de criação de conta: {} - Erro: {}", 
                                        command.getCommandId(), exception.getMessage());
                        }
                    });
        } catch (Exception e) {
            logger.error("Erro no handler de criação de conta: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Processa comando de crédito
     */
    public TransactionResponse handle(CreditCommand command) {
        logger.info("Processando comando de crédito: {}", command.getCommandId());
        
        try {
            TransactionRequest request = new TransactionRequest();
            request.setAccountId(command.getAccountId());
            request.setAmount(command.getAmount());
            
            TransactionResponse response = resilientAccountService.creditResilient(request);
            
            logger.info("Comando de crédito processado: {} com status: {}", 
                       command.getCommandId(), response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Erro no handler de crédito: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Processa comando de débito
     */
    public TransactionResponse handle(DebitCommand command) {
        logger.info("Processando comando de débito: {}", command.getCommandId());
        
        try {
            TransactionRequest request = new TransactionRequest();
            request.setAccountId(command.getAccountId());
            request.setAmount(command.getAmount());
            
            TransactionResponse response = resilientAccountService.debitResilient(request);
            
            logger.info("Comando de débito processado: {} com status: {}", 
                       command.getCommandId(), response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Erro no handler de débito: {}", e.getMessage(), e);
            throw e;
        }
    }
}