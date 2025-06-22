package com.bank.BankingSystemApplication.application.cqrs.command;

import com.bank.BankingSystemApplication.domain.model.TransactionRequest;

/**
 * Command específico para operações de débito
 */
public class DebitCommand extends TransactionCommand {
    
    public DebitCommand(TransactionRequest request) {
        super(request, TransactionType.DEBIT);
    }
    
    public DebitCommand(TransactionRequest request, String correlationId) {
        super(request, TransactionType.DEBIT, correlationId);
    }
}