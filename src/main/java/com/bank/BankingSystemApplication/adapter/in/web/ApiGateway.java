package com.bank.BankingSystemApplication.adapter.in.web;

import com.bank.BankingSystemApplication.adapter.in.web.BankingController;
import com.bank.BankingSystemApplication.adapter.out.messaging.AsyncBankingAdapter;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.dto.response.SuccessResponse;
import com.bank.BankingSystemApplication.dto.response.ErrorResponse;
import com.bank.BankingSystemApplication.infrastructure.monitoring.SystemLoadMonitor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/gateway")
@Tag(name = "API Gateway", description = "Gateway inteligente com roteamento baseado em carga do sistema")
public class ApiGateway {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiGateway.class);
    
    @Autowired
    private SystemLoadMonitor loadMonitor;
    
    @Autowired
    private BankingController syncController;
    
    @Autowired
    private AsyncBankingAdapter asyncAdapter;
    
    @PostMapping("/accounts")
    @Operation(summary = "Criar conta bancária", 
               description = "Cria uma nova conta bancária com roteamento inteligente baseado na carga do sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Conta criada com sucesso (processamento síncrono)"),
        @ApiResponse(responseCode = "202", description = "Solicitação aceita para processamento assíncrono"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Conta já existe para o CPF informado"),
        @ApiResponse(responseCode = "422", description = "Erro de regra de negócio"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<SuccessResponse<?>> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        String requestId = generateRequestId();
        logger.info("Gateway routing account creation request - RequestId: {}", requestId);
        
        if (loadMonitor.shouldUseAsyncProcessing()) {
            logger.info("High load detected, routing to async processing - RequestId: {}", requestId);
            CompletableFuture<String> asyncResult = asyncAdapter.createAccountAsync(request);
            
            Map<String, String> asyncInfo = Map.of(
                "requestId", requestId,
                "status", "PROCESSING",
                "estimatedTime", "30-60 seconds"
            );
            
            SuccessResponse<Map<String, String>> response = SuccessResponse.created(
                asyncInfo, 
                "Solicitação aceita para processamento assíncrono"
            );
            response.setRequestId(requestId);
            
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        } else {
            logger.info("Normal load, routing to sync processing - RequestId: {}", requestId);
            ResponseEntity<?> syncResponse = syncController.createAccount(request);
            
            if (syncResponse.getStatusCode() == HttpStatus.CREATED) {
                SuccessResponse<Object> response = SuccessResponse.created(
                    syncResponse.getBody(),
                    "Conta criada com sucesso"
                );
                response.setRequestId(requestId);
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            }
            
            // Para outros status codes, retorna a resposta original
            return (ResponseEntity<SuccessResponse<?>>) syncResponse;
        }
    }
    
    @PostMapping("/transactions/credit")
    @Operation(summary = "Operação de crédito", 
               description = "Realiza operação de crédito em conta bancária")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Crédito processado com sucesso"),
        @ApiResponse(responseCode = "202", description = "Solicitação aceita para processamento assíncrono"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Erro de regra de negócio"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<SuccessResponse<?>> credit(@Valid @RequestBody TransactionRequest request) {
        String requestId = generateRequestId();
        logger.info("Gateway routing credit transaction request - RequestId: {} - Account: {} - Amount: {}", 
            requestId, request.getAccountId(), request.getAmount());
        
        if (loadMonitor.shouldUseAsyncProcessing()) {
            logger.info("High load detected, routing to async processing - RequestId: {}", requestId);
            CompletableFuture<String> asyncResult = asyncAdapter.processCreditAsync(request);
            
            Map<String, Object> asyncInfo = Map.of(
                "requestId", requestId,
                "accountId", request.getAccountId(),
                "amount", request.getAmount(),
                "status", "PROCESSING"
            );
            
            SuccessResponse<Map<String, Object>> response = SuccessResponse.ok(
                asyncInfo,
                "Operação de crédito aceita para processamento assíncrono"
            );
            response.setRequestId(requestId);
            
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        } else {
            logger.info("Normal load, routing to sync processing - RequestId: {}", requestId);
            ResponseEntity<?> syncResponse = syncController.credit(request);
            
            if (syncResponse.getStatusCode() == HttpStatus.OK) {
                SuccessResponse<Object> response = SuccessResponse.ok(
                    syncResponse.getBody(),
                    "Operação de crédito processada com sucesso"
                );
                response.setRequestId(requestId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            
            return (ResponseEntity<SuccessResponse<?>>) syncResponse;
        }
    }
    
    @PostMapping("/transactions/debit")
    @Operation(summary = "Operação de débito", 
               description = "Realiza operação de débito em conta bancária")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Débito processado com sucesso"),
        @ApiResponse(responseCode = "202", description = "Solicitação aceita para processamento assíncrono"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou erro de regra de negócio"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<SuccessResponse<?>> debit(@Valid @RequestBody TransactionRequest request) {
        String requestId = generateRequestId();
        logger.info("Gateway routing debit transaction request - RequestId: {} - Account: {} - Amount: {}", 
            requestId, request.getAccountId(), request.getAmount());
        
        if (loadMonitor.shouldUseAsyncProcessing()) {
            logger.info("High load detected, routing to async processing - RequestId: {}", requestId);
            CompletableFuture<String> asyncResult = asyncAdapter.processDebitAsync(request);
            
            Map<String, Object> asyncInfo = Map.of(
                "requestId", requestId,
                "accountId", request.getAccountId(),
                "amount", request.getAmount(),
                "status", "PROCESSING"
            );
            
            SuccessResponse<Map<String, Object>> response = SuccessResponse.ok(
                asyncInfo,
                "Operação de débito aceita para processamento assíncrono"
            );
            response.setRequestId(requestId);
            
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        } else {
            logger.info("Normal load, routing to sync processing - RequestId: {}", requestId);
            ResponseEntity<?> syncResponse = syncController.debit(request);
            
            if (syncResponse.getStatusCode() == HttpStatus.OK) {
                SuccessResponse<Object> response = SuccessResponse.ok(
                    syncResponse.getBody(),
                    "Operação de débito processada com sucesso"
                );
                response.setRequestId(requestId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            
            return (ResponseEntity<SuccessResponse<?>>) syncResponse;
        }
    }
    
    @GetMapping("/accounts/{id}")
    @Operation(summary = "Consultar conta", 
               description = "Consulta informações de uma conta bancária pelo ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conta encontrada"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<SuccessResponse<Account>> getAccount(@PathVariable Long id) {
        String requestId = generateRequestId();
        logger.info("Gateway routing account query request - RequestId: {} - AccountId: {}", requestId, id);
        
        ResponseEntity<Account> syncResponse = syncController.getAccount(id);
        
        if (syncResponse.getStatusCode() == HttpStatus.OK) {
            SuccessResponse<Account> response = SuccessResponse.ok(
                syncResponse.getBody(),
                "Conta encontrada com sucesso"
            );
            response.setRequestId(requestId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        
        // Para outros status codes (404, etc), o GlobalExceptionHandler irá tratar
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @GetMapping("/load-status")
    @Operation(summary = "Status da carga do sistema", 
               description = "Consulta o status atual da carga do sistema e modo de processamento")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getLoadStatus() {
        String requestId = generateRequestId();
        double cpuUsage = loadMonitor.getCurrentCpuUsage();
        int activeConnections = loadMonitor.getActiveConnections();
        boolean useAsync = loadMonitor.shouldUseAsyncProcessing();
        
        Map<String, Object> loadInfo = Map.of(
            "cpuUsage", cpuUsage,
            "activeConnections", activeConnections,
            "processingMode", useAsync ? "ASYNC" : "SYNC",
            "timestamp", java.time.LocalDateTime.now()
        );
        
        SuccessResponse<Map<String, Object>> response = SuccessResponse.ok(
            loadInfo,
            "Status da carga do sistema obtido com sucesso"
        );
        response.setRequestId(requestId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gera um ID único para rastreamento da requisição.
     */
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().substring(0, 8);
    }
}