package com.bank.BankingSystemApplication.exception;

/**
 * Classe base para exceções de regras de negócio.
 * Deve resultar em HTTP 422 UNPROCESSABLE_ENTITY.
 */
public abstract class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Retorna o código específico do erro para identificação.
     */
    public abstract String getErrorCode();
}