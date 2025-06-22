package com.bank.BankingSystemApplication.application.service;

import com.bank.BankingSystemApplication.domain.model.*;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.application.service.kafka.TransactionEventProducer;
import com.bank.BankingSystemApplication.application.service.AccountService;
import com.bank.BankingSystemApplication.application.service.AsyncAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncAccountServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionEventProducer eventProducer;

    @InjectMocks
    private AsyncAccountService asyncAccountService;

    private AccountCreationRequest accountCreationRequest;
    private Account mockAccount;
    private TransactionRequest transactionRequest;

    @BeforeEach
    void setUp() {
        accountCreationRequest = new AccountCreationRequest();
        accountCreationRequest.setName("João Silva");
        accountCreationRequest.setCpf("12345678901");
        accountCreationRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        accountCreationRequest.setEmail("joao@email.com");
        accountCreationRequest.setPhone("11987654321");

        mockAccount = new Account();
        mockAccount.setId(1L);
        mockAccount.setName("João Silva");
        mockAccount.setCpf("12345678901");
        mockAccount.setBirthDate(LocalDate.of(1990, 1, 1));
        mockAccount.setEmail("joao@email.com");
        mockAccount.setPhone("11987654321");
        mockAccount.setBalance(BigDecimal.ZERO);

        transactionRequest = new TransactionRequest();
        transactionRequest.setAccountId(1L);
        transactionRequest.setAmount(new BigDecimal("100.00"));
    }

    @Test
    void testCreateAccountAsyncSuccess() {
        // Arrange
        when(accountService.createAccount(accountCreationRequest)).thenReturn(mockAccount);

        // Act
        Account result = asyncAccountService.createAccountAsync(accountCreationRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("João Silva", result.getName());

        // Verify that events were published
        verify(eventProducer, times(1)).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishNotificationEvent(any(NotificationEvent.class));
        verify(accountService, times(1)).createAccount(accountCreationRequest);
    }

    @Test
    void testCreateAccountAsyncWithoutEmail() {
        // Arrange
        accountCreationRequest.setEmail(null);
        mockAccount.setEmail(null);
        when(accountService.createAccount(accountCreationRequest)).thenReturn(mockAccount);

        // Act
        Account result = asyncAccountService.createAccountAsync(accountCreationRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // Verify audit event was published but not notification (no email)
        verify(eventProducer, times(1)).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, never()).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void testCreateAccountAsyncFailure() {
        // Arrange
        when(accountService.createAccount(accountCreationRequest))
                .thenThrow(new IllegalArgumentException("CPF já cadastrado"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            asyncAccountService.createAccountAsync(accountCreationRequest);
        });

        // Verify no events were published on failure
        verify(eventProducer, never()).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, never()).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void testCreditAsyncSuccess() {
        // Arrange
        TransactionResponse successResponse = new TransactionResponse(Status.EFETUADO, "Crédito efetuado com sucesso");
        mockAccount.setBalance(new BigDecimal("100.00"));
        
        when(accountService.credit(transactionRequest)).thenReturn(successResponse);
        when(accountService.getAccountById(1L)).thenReturn(mockAccount);

        // Act
        TransactionResponse result = asyncAccountService.creditAsync(transactionRequest);

        // Assert
        assertEquals(Status.EFETUADO, result.getStatus());
        assertEquals("Crédito efetuado com sucesso", result.getMessage());

        // Verify all events were published
        verify(eventProducer, times(1)).publishTransactionEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void testCreditAsyncFailure() {
        // Arrange
        TransactionResponse failureResponse = new TransactionResponse(Status.RECUSADO, "Conta não encontrada");
        
        when(accountService.credit(transactionRequest)).thenReturn(failureResponse);

        // Act
        TransactionResponse result = asyncAccountService.creditAsync(transactionRequest);

        // Assert
        assertEquals(Status.RECUSADO, result.getStatus());
        assertEquals("Conta não encontrada", result.getMessage());

        // Verify transaction and audit events were published but no notification for failed transaction
        verify(eventProducer, times(1)).publishTransactionEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, never()).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void testDebitAsyncSuccess() {
        // Arrange
        TransactionResponse successResponse = new TransactionResponse(Status.EFETUADO, "Débito efetuado com sucesso");
        mockAccount.setBalance(new BigDecimal("50.00"));
        
        when(accountService.debit(transactionRequest)).thenReturn(successResponse);
        when(accountService.getAccountById(1L)).thenReturn(mockAccount);

        // Act
        TransactionResponse result = asyncAccountService.debitAsync(transactionRequest);

        // Assert
        assertEquals(Status.EFETUADO, result.getStatus());
        assertEquals("Débito efetuado com sucesso", result.getMessage());

        // Verify all events were published
        verify(eventProducer, times(1)).publishTransactionEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void testDebitAsyncInsufficientFunds() {
        // Arrange
        TransactionResponse failureResponse = new TransactionResponse(Status.RECUSADO, "Saldo insuficiente");
        
        when(accountService.debit(transactionRequest)).thenReturn(failureResponse);
        when(accountService.getAccountById(1L)).thenReturn(mockAccount);

        // Act
        TransactionResponse result = asyncAccountService.debitAsync(transactionRequest);

        // Assert
        assertEquals(Status.RECUSADO, result.getStatus());
        assertEquals("Saldo insuficiente", result.getMessage());

        // Verify all events were published including failure notification
        verify(eventProducer, times(1)).publishTransactionEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void testDebitAsyncWithoutEmail() {
        // Arrange
        TransactionResponse successResponse = new TransactionResponse(Status.EFETUADO, "Débito efetuado com sucesso");
        mockAccount.setEmail(null);
        mockAccount.setBalance(new BigDecimal("50.00"));
        
        when(accountService.debit(transactionRequest)).thenReturn(successResponse);
        when(accountService.getAccountById(1L)).thenReturn(mockAccount);

        // Act
        TransactionResponse result = asyncAccountService.debitAsync(transactionRequest);

        // Assert
        assertEquals(Status.EFETUADO, result.getStatus());

        // Verify transaction and audit events were published but no notification (no email)
        verify(eventProducer, times(1)).publishTransactionEvent(any(TransactionEvent.class));
        verify(eventProducer, times(1)).publishAuditEvent(any(TransactionEvent.class));
        verify(eventProducer, never()).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void testAsyncOperationWithEventPublisherException() {
        // Arrange
        when(accountService.createAccount(accountCreationRequest)).thenReturn(mockAccount);
        doThrow(new RuntimeException("Kafka error")).when(eventProducer).publishAuditEvent(any(TransactionEvent.class));
        
        // Act & Assert - The service currently propagates event publishing exceptions
        assertThrows(RuntimeException.class, () -> {
            asyncAccountService.createAccountAsync(accountCreationRequest);
        });

        verify(accountService, times(1)).createAccount(accountCreationRequest);
    }
}