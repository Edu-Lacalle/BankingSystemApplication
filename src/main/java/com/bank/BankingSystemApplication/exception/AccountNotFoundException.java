package com.bank.BankingSystemApplication.exception;

/**
 * Exceção lançada quando uma conta não é encontrada.
 * Deve resultar em HTTP 404 NOT_FOUND.
 */
public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String accountId) {
        super(String.format("Conta não encontrada com ID: %s", accountId));
    }
    
    public String getErrorCode() {
        return "ACCOUNT_NOT_FOUND";
    }
}