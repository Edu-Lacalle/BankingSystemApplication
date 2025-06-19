package com.bank.BankingSystemApplication.service;

import com.bank.BankingSystemApplication.dto.AccountCreationRequest;
import com.bank.BankingSystemApplication.dto.Status;
import com.bank.BankingSystemApplication.dto.TransactionRequest;
import com.bank.BankingSystemApplication.dto.TransactionResponse;
import com.bank.BankingSystemApplication.entity.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AccountServiceTest {
    
    @Autowired
    private AccountService accountService;
    
    @Test
    public void testCreateAccountSuccess() {
        AccountCreationRequest request = new AccountCreationRequest();
        request.setName("João Silva");
        request.setCpf("12345678901");
        request.setBirthDate(LocalDate.of(1990, 1, 1));
        request.setEmail("joao@email.com");
        
        Account account = accountService.createAccount(request);
        
        assertNotNull(account.getId());
        assertEquals("João Silva", account.getName());
        assertEquals("12345678901", account.getCpf());
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }
    
    @Test
    public void testCreateAccountUnderAge() {
        AccountCreationRequest request = new AccountCreationRequest();
        request.setName("João Silva");
        request.setCpf("12345678901");
        request.setBirthDate(LocalDate.of(2010, 1, 1));
        
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount(request);
        });
    }
    
    @Test
    public void testCreditOperation() {
        AccountCreationRequest createRequest = new AccountCreationRequest();
        createRequest.setName("Maria Silva");
        createRequest.setCpf("98765432101");
        createRequest.setBirthDate(LocalDate.of(1985, 5, 15));
        
        Account account = accountService.createAccount(createRequest);
        
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setAccountId(account.getId());
        transactionRequest.setAmount(new BigDecimal("100.00"));
        
        TransactionResponse response = accountService.credit(transactionRequest);
        
        assertEquals(Status.EFETUADO, response.getStatus());
        
        Account updatedAccount = accountService.getAccountById(account.getId());
        assertEquals(new BigDecimal("100.00"), updatedAccount.getBalance());
    }
    
    @Test
    public void testDebitOperationSuccess() {
        AccountCreationRequest createRequest = new AccountCreationRequest();
        createRequest.setName("Pedro Silva");
        createRequest.setCpf("11223344556");
        createRequest.setBirthDate(LocalDate.of(1980, 3, 20));
        
        Account account = accountService.createAccount(createRequest);
        
        TransactionRequest creditRequest = new TransactionRequest();
        creditRequest.setAccountId(account.getId());
        creditRequest.setAmount(new BigDecimal("200.00"));
        accountService.credit(creditRequest);
        
        TransactionRequest debitRequest = new TransactionRequest();
        debitRequest.setAccountId(account.getId());
        debitRequest.setAmount(new BigDecimal("50.00"));
        
        TransactionResponse response = accountService.debit(debitRequest);
        
        assertEquals(Status.EFETUADO, response.getStatus());
        
        Account updatedAccount = accountService.getAccountById(account.getId());
        assertEquals(new BigDecimal("150.00"), updatedAccount.getBalance());
    }
    
    @Test
    public void testDebitOperationInsufficientFunds() {
        AccountCreationRequest createRequest = new AccountCreationRequest();
        createRequest.setName("Ana Silva");
        createRequest.setCpf("55667788990");
        createRequest.setBirthDate(LocalDate.of(1995, 8, 10));
        
        Account account = accountService.createAccount(createRequest);
        
        TransactionRequest debitRequest = new TransactionRequest();
        debitRequest.setAccountId(account.getId());
        debitRequest.setAmount(new BigDecimal("100.00"));
        
        TransactionResponse response = accountService.debit(debitRequest);
        
        assertEquals(Status.RECUSADO, response.getStatus());
        
        Account updatedAccount = accountService.getAccountById(account.getId());
        assertEquals(BigDecimal.ZERO, updatedAccount.getBalance());
    }
}