package com.bank.BankingSystemApplication.adapter.out.persistence;

import com.bank.BankingSystemApplication.domain.port.out.AccountPersistencePort;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.infrastructure.persistence.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AccountPersistenceAdapter implements AccountPersistencePort {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Override
    public Account save(Account account) {
        return accountRepository.save(account);
    }
    
    @Override
    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }
    
    @Override
    public Optional<Account> findByCpf(String cpf) {
        return accountRepository.findByCpf(cpf);
    }
    
    @Override
    public Optional<Account> findByIdForUpdate(Long id) {
        return accountRepository.findByIdForUpdate(id);
    }
}