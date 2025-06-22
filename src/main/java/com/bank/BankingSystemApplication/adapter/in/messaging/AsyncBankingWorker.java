package com.bank.BankingSystemApplication.adapter.in.messaging;

import com.bank.BankingSystemApplication.domain.port.in.BankingUseCase;
import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AsyncBankingWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncBankingWorker.class);
    
    @Autowired
    private BankingUseCase bankingUseCase;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = "banking.account.create", groupId = "banking-worker")
    public void processAccountCreation(AccountCreationRequest request) {
        logger.info("Worker: Processing async account creation");
        
        try {
            Account account = bankingUseCase.createAccount(request);
            logger.info("Worker: Account created successfully with ID: {}", account.getId());
            
            kafkaTemplate.send("banking.account.created", account);
            
        } catch (Exception e) {
            logger.error("Worker: Failed to create account: {}", e.getMessage(), e);
            kafkaTemplate.send("banking.account.failed", 
                             String.format("Account creation failed: %s", e.getMessage()));
        }
    }
    
    @KafkaListener(topics = "banking.transaction.credit", groupId = "banking-worker")
    public void processCreditTransaction(TransactionRequest request) {
        logger.info("Worker: Processing async credit transaction");
        
        try {
            TransactionResponse response = bankingUseCase.credit(request);
            logger.info("Worker: Credit processed with status: {}", response.getStatus());
            
            kafkaTemplate.send("banking.transaction.processed", response);
            
        } catch (Exception e) {
            logger.error("Worker: Failed to process credit: {}", e.getMessage(), e);
            kafkaTemplate.send("banking.transaction.failed", 
                             String.format("Credit failed: %s", e.getMessage()));
        }
    }
    
    @KafkaListener(topics = "banking.transaction.debit", groupId = "banking-worker")
    public void processDebitTransaction(TransactionRequest request) {
        logger.info("Worker: Processing async debit transaction");
        
        try {
            TransactionResponse response = bankingUseCase.debit(request);
            logger.info("Worker: Debit processed with status: {}", response.getStatus());
            
            kafkaTemplate.send("banking.transaction.processed", response);
            
        } catch (Exception e) {
            logger.error("Worker: Failed to process debit: {}", e.getMessage(), e);
            kafkaTemplate.send("banking.transaction.failed", 
                             String.format("Debit failed: %s", e.getMessage()));
        }
    }
}