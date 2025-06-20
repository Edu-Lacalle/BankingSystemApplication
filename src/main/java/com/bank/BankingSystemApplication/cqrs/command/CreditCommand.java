package com.bank.BankingSystemApplication.cqrs.command;

import com.bank.BankingSystemApplication.dto.TransactionRequest;

/**
 * Command específico para operações de crédito
 */
public class CreditCommand extends TransactionCommand {
    
    public CreditCommand(TransactionRequest request) {
        super(request, TransactionType.CREDIT);
    }
    
    public CreditCommand(TransactionRequest request, String correlationId) {
        super(request, TransactionType.CREDIT, correlationId);
    }
}