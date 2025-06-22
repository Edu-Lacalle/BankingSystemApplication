package com.bank.BankingSystemApplication.domain.port.in;

import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;

public interface BankingUseCase {
    Account createAccount(AccountCreationRequest request);
    TransactionResponse credit(TransactionRequest request);
    TransactionResponse debit(TransactionRequest request);
    Account getAccountById(Long id);
}