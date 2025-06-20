package com.bank.BankingSystemApplication.cqrs.handler;

import com.bank.BankingSystemApplication.cqrs.query.AccountQuery;
import com.bank.BankingSystemApplication.entity.Account;
import com.bank.BankingSystemApplication.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler para queries de conta seguindo o padrão CQRS
 * Responsável apenas por operações de leitura (Query)
 */
@Component
public class AccountQueryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountQueryHandler.class);
    
    @Autowired
    private AccountService accountService;
    
    /**
     * Processa query de consulta de conta
     */
    public Account handle(AccountQuery query) {
        logger.info("Processando query de conta: {} para ID: {}", 
                   query.getQueryId(), query.getAccountId());
        
        try {
            Account account = accountService.getAccountById(query.getAccountId());
            
            logger.info("Query de conta processada com sucesso: {} para conta: {}", 
                       query.getQueryId(), account.getId());
            
            return account;
            
        } catch (Exception e) {
            logger.error("Erro no handler de query de conta: {} - {}", 
                        query.getQueryId(), e.getMessage(), e);
            throw e;
        }
    }
}