package com.bank.BankingSystemApplication.service;

import com.bank.BankingSystemApplication.dto.AccountCreationRequest;
import com.bank.BankingSystemApplication.dto.Status;
import com.bank.BankingSystemApplication.dto.TransactionRequest;
import com.bank.BankingSystemApplication.dto.TransactionResponse;
import com.bank.BankingSystemApplication.entity.Account;
import com.bank.BankingSystemApplication.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Service
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Transactional
    public Account createAccount(AccountCreationRequest request) {
        validateAge(request.getBirthDate());
        
        if (accountRepository.findByCpf(request.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
        
        Account account = new Account();
        account.setName(request.getName());
        account.setCpf(request.getCpf());
        account.setBirthDate(request.getBirthDate());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        
        return accountRepository.save(account);
    }
    
    @Transactional
    public TransactionResponse credit(TransactionRequest request) {
        try {
            Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
            
            account.setBalance(account.getBalance().add(request.getAmount()));
            accountRepository.save(account);
            
            return new TransactionResponse(Status.EFETUADO,
                    "Crédito efetuado com sucesso");
        } catch (Exception e) {
            return new TransactionResponse(Status.RECUSADO,
                    "Erro ao processar crédito: " + e.getMessage());
        }
    }
    
    @Transactional
    public TransactionResponse debit(TransactionRequest request) {
        try {
            Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
            
            BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
            
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                return new TransactionResponse(Status.RECUSADO,
                        "Saldo insuficiente");
            }
            
            account.setBalance(newBalance);
            accountRepository.save(account);
            
            return new TransactionResponse(Status.EFETUADO,
                    "Débito efetuado com sucesso");
        } catch (Exception e) {
            return new TransactionResponse(Status.RECUSADO,
                    "Erro ao processar débito: " + e.getMessage());
        }
    }
    
    private void validateAge(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("Idade mínima de 18 anos não atendida");
        }
    }
    
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }
}