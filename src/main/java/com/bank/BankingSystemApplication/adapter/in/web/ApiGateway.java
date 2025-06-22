package com.bank.BankingSystemApplication.adapter.in.web;

import com.bank.BankingSystemApplication.adapter.in.web.BankingController;
import com.bank.BankingSystemApplication.adapter.out.messaging.AsyncBankingAdapter;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.infrastructure.monitoring.SystemLoadMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/gateway")
public class ApiGateway {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiGateway.class);
    
    @Autowired
    private SystemLoadMonitor loadMonitor;
    
    @Autowired
    private BankingController syncController;
    
    @Autowired
    private AsyncBankingAdapter asyncAdapter;
    
    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(@RequestBody AccountCreationRequest request) {
        logger.info("Gateway routing account creation request");
        
        if (loadMonitor.shouldUseAsyncProcessing()) {
            logger.info("High load detected, routing to async processing");
            CompletableFuture<String> asyncResult = asyncAdapter.createAccountAsync(request);
            return ResponseEntity.accepted().body("Request accepted for async processing. Check status endpoint.");
        } else {
            logger.info("Normal load, routing to sync processing");
            return syncController.createAccount(request);
        }
    }
    
    @PostMapping("/transactions/credit")
    public ResponseEntity<?> credit(@RequestBody TransactionRequest request) {
        logger.info("Gateway routing credit transaction request");
        
        if (loadMonitor.shouldUseAsyncProcessing()) {
            logger.info("High load detected, routing to async processing");
            CompletableFuture<String> asyncResult = asyncAdapter.processCreditAsync(request);
            return ResponseEntity.accepted().body("Credit request accepted for async processing");
        } else {
            logger.info("Normal load, routing to sync processing");
            return syncController.credit(request);
        }
    }
    
    @PostMapping("/transactions/debit")
    public ResponseEntity<?> debit(@RequestBody TransactionRequest request) {
        logger.info("Gateway routing debit transaction request");
        
        if (loadMonitor.shouldUseAsyncProcessing()) {
            logger.info("High load detected, routing to async processing");
            CompletableFuture<String> asyncResult = asyncAdapter.processDebitAsync(request);
            return ResponseEntity.accepted().body("Debit request accepted for async processing");
        } else {
            logger.info("Normal load, routing to sync processing");
            return syncController.debit(request);
        }
    }
    
    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        return syncController.getAccount(id);
    }
    
    @GetMapping("/load-status")
    public ResponseEntity<String> getLoadStatus() {
        double cpuUsage = loadMonitor.getCurrentCpuUsage();
        int activeConnections = loadMonitor.getActiveConnections();
        boolean useAsync = loadMonitor.shouldUseAsyncProcessing();
        
        String status = String.format(
            "CPU: %.2f%%, Active Connections: %d, Processing Mode: %s",
            cpuUsage, activeConnections, useAsync ? "ASYNC" : "SYNC"
        );
        
        return ResponseEntity.ok(status);
    }
}