package com.bank.BankingSystemApplication.adapter.in.web;

import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.Status;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.application.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller REST para operações bancárias básicas.
 * 
 * Este controller expõe endpoints para:
 * - Criação de contas bancárias
 * - Operações de crédito
 * - Operações de débito
 * - Consulta de contas
 * 
 * Características:
 * - Validação automática de dados de entrada
 * - Tratamento de exceções com códigos HTTP apropriados
 * - Documentação OpenAPI/Swagger integrada
 * - Respostas padronizadas com status HTTP
 * 
 * Endpoints disponíveis:
 * - POST /api/accounts - Criar conta
 * - POST /api/accounts/credit - Creditar valor
 * - POST /api/accounts/debit - Debitar valor
 * - GET /api/accounts/{id} - Consultar conta
 * 
 * @author Sistema Bancário
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/accounts")
@Validated
@Tag(name = "Account Management", description = "API for managing bank accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @PostMapping
    @Operation(summary = "Create new account", description = "Creates a new bank account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        try {
            Account account = accountService.createAccount(request);
            return new ResponseEntity<>(account, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/credit")
    @Operation(summary = "Credit account", description = "Credits amount to an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Credit transaction successful"),
        @ApiResponse(responseCode = "400", description = "Invalid transaction request")
    })
    public ResponseEntity<TransactionResponse> credit(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = accountService.credit(request);
        HttpStatus status = response.getStatus() == Status.EFETUADO
                ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(response, status);
    }
    
    @PostMapping("/debit")
    @Operation(summary = "Debit account", description = "Debits amount from an account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Debit transaction successful"),
        @ApiResponse(responseCode = "400", description = "Invalid transaction request or insufficient funds")
    })
    public ResponseEntity<TransactionResponse> debit(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = accountService.debit(request);
        HttpStatus status = response.getStatus() == Status.EFETUADO
                ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(response, status);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID", description = "Retrieves account information by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account found"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAccount(@Parameter(description = "Account ID") @PathVariable Long id) {
        try {
            Account account = accountService.getAccountById(id);
            return new ResponseEntity<>(account, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}