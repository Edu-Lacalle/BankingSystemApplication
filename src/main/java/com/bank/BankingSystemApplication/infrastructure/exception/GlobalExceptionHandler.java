package com.bank.BankingSystemApplication.infrastructure.exception;

import com.bank.BankingSystemApplication.dto.response.ErrorResponse;
import com.bank.BankingSystemApplication.exception.AccountNotFoundException;
import com.bank.BankingSystemApplication.exception.BusinessException;
import com.bank.BankingSystemApplication.exception.DuplicateAccountException;
import com.bank.BankingSystemApplication.exception.InsufficientFundsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Trata exceções de validação de campos da requisição.
     * Retorna HTTP 400 BAD_REQUEST com detalhes dos campos inválidos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Dados de entrada inválidos. Verifique os campos informados.",
            getPath(request),
            requestId,
            fieldErrors
        );
        
        logger.warn("Validation error - RequestId: {} - Fields: {}", requestId, fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Trata exceções de conta não encontrada.
     * Retorna HTTP 404 NOT_FOUND.
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(
            AccountNotFoundException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request),
            requestId
        );
        
        logger.warn("Account not found - RequestId: {} - Message: {}", requestId, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Trata exceções de conta duplicada.
     * Retorna HTTP 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAccountException(
            DuplicateAccountException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request),
            requestId
        );
        
        logger.warn("Duplicate account attempt - RequestId: {} - Message: {}", requestId, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * Trata exceções de saldo insuficiente.
     * Retorna HTTP 422 UNPROCESSABLE_ENTITY.
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
            InsufficientFundsException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request),
            requestId
        );
        
        logger.warn("Insufficient funds - RequestId: {} - Message: {}", requestId, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    /**
     * Trata exceções genéricas de regras de negócio.
     * Retorna HTTP 422 UNPROCESSABLE_ENTITY.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request),
            requestId
        );
        
        logger.warn("Business rule violation - RequestId: {} - Code: {} - Message: {}", 
            requestId, ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    /**
     * Trata conflitos de concorrência otimista.
     * Retorna HTTP 412 PRECONDITION_FAILED.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            "CONCURRENT_MODIFICATION",
            "O recurso foi modificado por outro processo. Tente novamente com os dados atualizados.",
            getPath(request),
            requestId
        );
        
        logger.warn("Optimistic locking failure - RequestId: {} - Message: {}", requestId, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.PRECONDITION_FAILED);
    }
    
    /**
     * Trata argumentos ilegais na requisição.
     * Retorna HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            getPath(request),
            requestId
        );
        
        logger.warn("Invalid argument - RequestId: {} - Message: {}", requestId, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Trata JSON malformado na requisição.
     * Retorna HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            "MALFORMED_JSON",
            "Formato JSON inválido. Verifique a sintaxe dos dados enviados.",
            getPath(request),
            requestId
        );
        
        logger.warn("Malformed JSON - RequestId: {} - Message: {}", requestId, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Trata exceções genéricas não mapeadas.
     * Retorna HTTP 500 INTERNAL_SERVER_ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "Erro interno do servidor. Tente novamente mais tarde.",
            getPath(request),
            requestId
        );
        
        logger.error("Unexpected error - RequestId: {} - Exception: {}", requestId, ex.getClass().getSimpleName(), ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Gera um ID único para rastreamento da requisição.
     */
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Extrai o caminho da requisição.
     */
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}