package com.bank.BankingSystemApplication.domain.port.out;

import com.bank.BankingSystemApplication.domain.model.Account;

import java.util.Optional;

public interface AccountPersistencePort {
    Account save(Account account);
    Optional<Account> findById(Long id);
    Optional<Account> findByCpf(String cpf);
    Optional<Account> findByIdForUpdate(Long id);
}