package com.bank.BankingSystemApplication.adapter.in.web;

import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.application.service.AsyncAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts/async")
@Validated
@Tag(name = "Async Account Management", description = "API for asynchronous account operations with Kafka events")
public class AsyncAccountController {
    
    @Autowired
    private AsyncAccountService asyncAccountService;
    
    @PostMapping
    @Operation(summary = "Create new account (async)", 
               description = "Creates a new bank account and publishes events for notifications and audit")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created successfully with async events"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createAccountAsync(@Valid @RequestBody AccountCreationRequest request) {
        try {
            Account account = asyncAccountService.createAccountAsync(request);
            return new ResponseEntity<>(account, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/credit")
    @Operation(summary = "Credit account (async)", 
               description = "Credits amount to an account and publishes events for notifications and audit")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Credit transaction successful with async events"),
        @ApiResponse(responseCode = "400", description = "Invalid transaction request")
    })
    public ResponseEntity<TransactionResponse> creditAsync(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = asyncAccountService.creditAsync(request);
        HttpStatus status = response.getStatus().name().equals("EFETUADO")
                ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(response, status);
    }
    
    @PostMapping("/debit")
    @Operation(summary = "Debit account (async)", 
               description = "Debits amount from an account and publishes events for notifications and audit")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Debit transaction successful with async events"),
        @ApiResponse(responseCode = "400", description = "Invalid transaction request or insufficient funds")
    })
    public ResponseEntity<TransactionResponse> debitAsync(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = asyncAccountService.debitAsync(request);
        HttpStatus status = response.getStatus().name().equals("EFETUADO")
                ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(response, status);
    }
}