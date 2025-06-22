package com.bank.BankingSystemApplication.adapter.out.messaging;

import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AsyncBankingAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncBankingAdapter.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public CompletableFuture<String> createAccountAsync(AccountCreationRequest request) {
        logger.info("Adapter: Sending async account creation request");
        
        return kafkaTemplate.send("banking.account.create", request)
                .thenApply(result -> {
                    logger.info("Account creation request sent successfully");
                    return "ACCEPTED";
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to send account creation request", throwable);
                    return "FAILED";
                });
    }
    
    public CompletableFuture<String> processCreditAsync(TransactionRequest request) {
        logger.info("Adapter: Sending async credit request");
        
        return kafkaTemplate.send("banking.transaction.credit", request)
                .thenApply(result -> {
                    logger.info("Credit request sent successfully");
                    return "ACCEPTED";
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to send credit request", throwable);
                    return "FAILED";
                });
    }
    
    public CompletableFuture<String> processDebitAsync(TransactionRequest request) {
        logger.info("Adapter: Sending async debit request");
        
        return kafkaTemplate.send("banking.transaction.debit", request)
                .thenApply(result -> {
                    logger.info("Debit request sent successfully");
                    return "ACCEPTED";
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to send debit request", throwable);
                    return "FAILED";
                });
    }
}