package com.bank.BankingSystemApplication.adapter.in.web;

import com.bank.BankingSystemApplication.domain.model.*;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.application.service.AsyncAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AsyncAccountController.class)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
    "performance.monitoring.enabled=false"
})
class AsyncAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AsyncAccountService asyncAccountService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void testCreateAccountAsyncSuccess() throws Exception {
        // Arrange
        when(asyncAccountService.createAccountAsync(any(AccountCreationRequest.class)))
                .thenReturn(mockAccount);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountCreationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void testCreateAccountAsyncBadRequest() throws Exception {
        // Arrange
        when(asyncAccountService.createAccountAsync(any(AccountCreationRequest.class)))
                .thenThrow(new IllegalArgumentException("CPF já cadastrado"));

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountCreationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("CPF já cadastrado"));
    }

    @Test
    void testCreateAccountAsyncInternalServerError() throws Exception {
        // Arrange
        when(asyncAccountService.createAccountAsync(any(AccountCreationRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountCreationRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro interno do servidor"));
    }

    @Test
    void testCreateAccountAsyncValidationError() throws Exception {
        // Arrange - Request with invalid data
        AccountCreationRequest invalidRequest = new AccountCreationRequest();
        invalidRequest.setName(""); // Invalid: empty name
        invalidRequest.setCpf("123"); // Invalid: CPF too short
        invalidRequest.setBirthDate(LocalDate.of(2010, 1, 1)); // Invalid: underage

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreditAsyncSuccess() throws Exception {
        // Arrange
        TransactionResponse successResponse = new TransactionResponse(Status.EFETUADO, "Crédito efetuado com sucesso");
        when(asyncAccountService.creditAsync(any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EFETUADO"))
                .andExpect(jsonPath("$.message").value("Crédito efetuado com sucesso"));
    }

    @Test
    void testCreditAsyncBadRequest() throws Exception {
        // Arrange
        TransactionResponse failureResponse = new TransactionResponse(Status.RECUSADO, "Conta não encontrada");
        when(asyncAccountService.creditAsync(any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.message").value("Conta não encontrada"));
    }

    @Test
    void testCreditAsyncValidationError() throws Exception {
        // Arrange - Invalid transaction request
        TransactionRequest invalidRequest = new TransactionRequest();
        invalidRequest.setAccountId(null); // Invalid: null account ID
        invalidRequest.setAmount(new BigDecimal("-100.00")); // Invalid: negative amount

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDebitAsyncSuccess() throws Exception {
        // Arrange
        TransactionResponse successResponse = new TransactionResponse(Status.EFETUADO, "Débito efetuado com sucesso");
        when(asyncAccountService.debitAsync(any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EFETUADO"))
                .andExpect(jsonPath("$.message").value("Débito efetuado com sucesso"));
    }

    @Test
    void testDebitAsyncInsufficientFunds() throws Exception {
        // Arrange
        TransactionResponse failureResponse = new TransactionResponse(Status.RECUSADO, "Saldo insuficiente");
        when(asyncAccountService.debitAsync(any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente"));
    }

    @Test
    void testDebitAsyncValidationError() throws Exception {
        // Arrange - Invalid transaction request
        TransactionRequest invalidRequest = new TransactionRequest();
        invalidRequest.setAccountId(1L);
        invalidRequest.setAmount(BigDecimal.ZERO); // Invalid: zero amount

        // Act & Assert
        mockMvc.perform(post("/api/accounts/async/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAsyncEndpointsWithMalformedJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/accounts/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/accounts/async/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/accounts/async/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
}