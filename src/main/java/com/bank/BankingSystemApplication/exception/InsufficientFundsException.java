package com.bank.BankingSystemApplication.exception;

/**
 * Exceção lançada quando uma operação de débito não pode ser realizada
 * devido a saldo insuficiente na conta.
 */
public class InsufficientFundsException extends BusinessException {
    
    public InsufficientFundsException(String accountId, String operationAmount, String currentBalance) {
        super(String.format("Saldo insuficiente na conta %s. Valor solicitado: %s, Saldo atual: %s", 
            accountId, operationAmount, currentBalance));
    }
    
    public InsufficientFundsException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return "INSUFFICIENT_FUNDS";
    }
}