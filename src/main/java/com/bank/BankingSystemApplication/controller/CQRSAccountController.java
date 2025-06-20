package com.bank.BankingSystemApplication.controller;

import com.bank.BankingSystemApplication.cqrs.command.CreateAccountCommand;
import com.bank.BankingSystemApplication.cqrs.command.CreditCommand;
import com.bank.BankingSystemApplication.cqrs.command.DebitCommand;
import com.bank.BankingSystemApplication.cqrs.handler.AccountCommandHandler;
import com.bank.BankingSystemApplication.cqrs.handler.AccountQueryHandler;
import com.bank.BankingSystemApplication.cqrs.query.AccountQuery;
import com.bank.BankingSystemApplication.dto.AccountCreationRequest;
import com.bank.BankingSystemApplication.dto.TransactionRequest;
import com.bank.BankingSystemApplication.dto.TransactionResponse;
import com.bank.BankingSystemApplication.entity.Account;
import com.bank.BankingSystemApplication.saga.TransferSaga;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Controller que implementa o padrão CQRS separando comandos de queries
 */
@RestController
@RequestMapping("/api/v2/accounts")
@Tag(name = "CQRS Account API", description = "API de contas bancárias seguindo o padrão CQRS")
public class CQRSAccountController {
    
    private static final Logger logger = LoggerFactory.getLogger(CQRSAccountController.class);
    
    @Autowired
    private AccountCommandHandler commandHandler;
    
    @Autowired
    private AccountQueryHandler queryHandler;
    
    @Autowired
    private TransferSaga transferSaga;
    
    /**
     * Comando: Criar nova conta
     */
    @PostMapping
    @Operation(summary = "Criar conta usando CQRS", description = "Cria uma nova conta bancária usando padrão CQRS")
    @RateLimiter(name = "banking-api")
    public CompletableFuture<ResponseEntity<Account>> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        logger.info("Recebido comando de criação de conta via CQRS para CPF: {}", request.getCpf());
        
        CreateAccountCommand command = new CreateAccountCommand(request);
        
        return commandHandler.handle(command)
                .thenApply(account -> {
                    logger.info("Conta criada via CQRS com sucesso: {}", account.getId());
                    return ResponseEntity.status(HttpStatus.CREATED).body(account);
                })
                .exceptionally(throwable -> {
                    logger.error("Erro ao criar conta via CQRS: {}", throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                });
    }
    
    /**
     * Comando: Operação de crédito
     */
    @PostMapping("/credit")
    @Operation(summary = "Creditar conta usando CQRS", description = "Realiza operação de crédito usando padrão CQRS")
    @RateLimiter(name = "banking-api")
    public ResponseEntity<TransactionResponse> creditAccount(@Valid @RequestBody TransactionRequest request) {
        logger.info("Recebido comando de crédito via CQRS para conta: {}", request.getAccountId());
        
        try {
            CreditCommand command = new CreditCommand(request);
            TransactionResponse response = commandHandler.handle(command);
            
            if (response.getStatus() == com.bank.BankingSystemApplication.dto.Status.EFETUADO) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar crédito via CQRS: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TransactionResponse(com.bank.BankingSystemApplication.dto.Status.RECUSADO, 
                                                "Erro interno do servidor"));
        }
    }
    
    /**
     * Comando: Operação de débito
     */
    @PostMapping("/debit")
    @Operation(summary = "Debitar conta usando CQRS", description = "Realiza operação de débito usando padrão CQRS")
    @RateLimiter(name = "banking-api")
    public ResponseEntity<TransactionResponse> debitAccount(@Valid @RequestBody TransactionRequest request) {
        logger.info("Recebido comando de débito via CQRS para conta: {}", request.getAccountId());
        
        try {
            DebitCommand command = new DebitCommand(request);
            TransactionResponse response = commandHandler.handle(command);
            
            if (response.getStatus() == com.bank.BankingSystemApplication.dto.Status.EFETUADO) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar débito via CQRS: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TransactionResponse(com.bank.BankingSystemApplication.dto.Status.RECUSADO, 
                                                "Erro interno do servidor"));
        }
    }
    
    /**
     * Query: Consultar conta
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar conta usando CQRS", description = "Consulta informações de uma conta usando padrão CQRS")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        logger.info("Recebida query de consulta via CQRS para conta: {}", id);
        
        try {
            AccountQuery query = new AccountQuery(id);
            Account account = queryHandler.handle(query);
            
            return ResponseEntity.ok(account);
            
        } catch (RuntimeException e) {
            logger.warn("Conta não encontrada via CQRS: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao consultar conta via CQRS: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Comando: Transferência usando Saga Pattern
     */
    @PostMapping("/transfer")
    @Operation(summary = "Transferir entre contas usando Saga", description = "Realiza transferência entre contas usando padrão Saga")
    @RateLimiter(name = "banking-api")
    public ResponseEntity<TransferSaga.SagaResult> transferBetweenAccounts(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam BigDecimal amount) {
        
        logger.info("Iniciando transferência Saga de {} para {} no valor de {}", 
                   fromAccountId, toAccountId, amount);
        
        try {
            TransferSaga.SagaResult result = transferSaga.executeTransfer(fromAccountId, toAccountId, amount);
            
            if (result.isSuccessful()) {
                logger.info("Transferência Saga concluída com sucesso: {}", result.getSagaId());
                return ResponseEntity.ok(result);
            } else {
                logger.warn("Transferência Saga falhou: {} - {}", result.getSagaId(), result.getErrorMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            logger.error("Erro crítico na transferência Saga: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}