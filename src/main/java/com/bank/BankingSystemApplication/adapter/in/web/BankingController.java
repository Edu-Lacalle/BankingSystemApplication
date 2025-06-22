package com.bank.BankingSystemApplication.adapter.in.web;

import com.bank.BankingSystemApplication.domain.port.in.BankingUseCase;
import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.infrastructure.monitoring.SystemLoadMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sync")
public class BankingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BankingController.class);
    
    @Autowired
    private BankingUseCase bankingUseCase;
    
    @Autowired
    private SystemLoadMonitor loadMonitor;
    
    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        loadMonitor.incrementConnections();
        try {
            logger.info("Adapter: Sync account creation request");
            Account account = bankingUseCase.createAccount(request);
            return ResponseEntity.ok(account);
        } finally {
            loadMonitor.decrementConnections();
        }
    }
    
    @PostMapping("/transactions/credit")
    public ResponseEntity<TransactionResponse> credit(@Valid @RequestBody TransactionRequest request) {
        loadMonitor.incrementConnections();
        try {
            logger.info("Adapter: Sync credit transaction request");
            TransactionResponse response = bankingUseCase.credit(request);
            return ResponseEntity.ok(response);
        } finally {
            loadMonitor.decrementConnections();
        }
    }
    
    @PostMapping("/transactions/debit")
    public ResponseEntity<TransactionResponse> debit(@Valid @RequestBody TransactionRequest request) {
        loadMonitor.incrementConnections();
        try {
            logger.info("Adapter: Sync debit transaction request");
            TransactionResponse response = bankingUseCase.debit(request);
            return ResponseEntity.ok(response);
        } finally {
            loadMonitor.decrementConnections();
        }
    }
    
    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        logger.info("Adapter: Get account request for ID: {}", id);
        Account account = bankingUseCase.getAccountById(id);
        return ResponseEntity.ok(account);
    }
}