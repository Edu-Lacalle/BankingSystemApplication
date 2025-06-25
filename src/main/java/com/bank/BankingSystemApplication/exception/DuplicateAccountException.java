package com.bank.BankingSystemApplication.exception;

/**
 * Exceção lançada quando se tenta criar uma conta que já existe
 * (baseado no CPF ou outro identificador único).
 */
public class DuplicateAccountException extends BusinessException {
    
    public DuplicateAccountException(String cpf) {
        super(String.format("Já existe uma conta cadastrada com o CPF: %s", cpf));
    }
    
    @Override
    public String getErrorCode() {
        return "DUPLICATE_ACCOUNT";
    }
}