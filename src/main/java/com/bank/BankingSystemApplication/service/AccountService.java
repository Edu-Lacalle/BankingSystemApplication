package com.bank.BankingSystemApplication.service;

import com.bank.BankingSystemApplication.audit.BankingAuditService;
import com.bank.BankingSystemApplication.dto.AccountCreationRequest;
import com.bank.BankingSystemApplication.dto.Status;
import com.bank.BankingSystemApplication.dto.TransactionRequest;
import com.bank.BankingSystemApplication.dto.TransactionResponse;
import com.bank.BankingSystemApplication.entity.Account;
import com.bank.BankingSystemApplication.metrics.BankingMetricsService;
import com.bank.BankingSystemApplication.repository.AccountRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
public class AccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private BankingMetricsService metricsService;
    
    @Autowired
    private BankingAuditService auditService;
    
    @Transactional
    public Account createAccount(AccountCreationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "createAccount");
        MDC.put("cpf", request.getCpf().substring(0, 3) + "*****" + request.getCpf().substring(8));
        
        Timer.Sample sample = metricsService.startAccountCreationTimer();
        
        try {
            logger.info("Iniciando criação de conta para CPF: {}", request.getCpf().substring(0, 3) + "***");
            
            validateAge(request.getBirthDate());
            
            if (accountRepository.findByCpf(request.getCpf()).isPresent()) {
                auditService.auditAccountCreation(null, request.getCpf(), request.getName(), 
                                                false, "CPF já cadastrado");
                throw new IllegalArgumentException("CPF já cadastrado");
            }
        
            Account account = new Account();
            account.setName(request.getName());
            account.setCpf(request.getCpf());
            account.setBirthDate(request.getBirthDate());
            account.setEmail(request.getEmail());
            account.setPhone(request.getPhone());
            
            Account savedAccount = accountRepository.save(account);
            
            // Registrar métricas
            metricsService.incrementAccountCreated();
            metricsService.recordAccountCreationTime(sample);
            
            // Auditoria
            auditService.auditAccountCreation(savedAccount.getId(), request.getCpf(), 
                                            request.getName(), true, "Conta criada com sucesso");
            
            logger.info("Conta criada com sucesso. ID: {}", savedAccount.getId());
            
            return savedAccount;
            
        } catch (Exception e) {
            metricsService.recordAccountCreationTime(sample);
            auditService.auditAccountCreation(null, request.getCpf(), request.getName(), 
                                            false, "Erro: " + e.getMessage());
            logger.error("Erro ao criar conta: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    @Transactional
    public TransactionResponse credit(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "credit");
        MDC.put("accountId", String.valueOf(request.getAccountId()));
        MDC.put("amount", request.getAmount().toString());
        
        Timer.Sample sample = metricsService.startTransactionTimer();
        
        try {
            logger.info("Iniciando operação de crédito para conta: {} no valor: {}", 
                       request.getAccountId(), request.getAmount());
            
            Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
            
            BigDecimal previousBalance = account.getBalance();
            account.setBalance(account.getBalance().add(request.getAmount()));
            accountRepository.save(account);
            
            // Registrar métricas
            metricsService.incrementCreditOperation();
            metricsService.incrementTransactionSuccess();
            metricsService.recordTransactionTime(sample);
            
            // Auditoria
            auditService.auditTransaction("CREDIT", request.getAccountId(), request.getAmount(), 
                                        true, String.format("Saldo anterior: %s, Novo saldo: %s", 
                                        previousBalance, account.getBalance()), correlationId);
            
            logger.info("Crédito efetuado com sucesso. Conta: {}, Novo saldo: {}", 
                       request.getAccountId(), account.getBalance());
            
            return new TransactionResponse(Status.EFETUADO, "Crédito efetuado com sucesso");
            
        } catch (Exception e) {
            metricsService.incrementTransactionFailure();
            metricsService.recordTransactionTime(sample);
            
            auditService.auditTransaction("CREDIT", request.getAccountId(), request.getAmount(), 
                                        false, "Erro: " + e.getMessage(), correlationId);
            
            logger.error("Erro ao processar crédito: {}", e.getMessage(), e);
            
            return new TransactionResponse(Status.RECUSADO, e.getMessage());
        } finally {
            MDC.clear();
        }
    }
    
    @Transactional
    public TransactionResponse debit(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "debit");
        MDC.put("accountId", String.valueOf(request.getAccountId()));
        MDC.put("amount", request.getAmount().toString());
        
        Timer.Sample sample = metricsService.startTransactionTimer();
        
        try {
            logger.info("Iniciando operação de débito para conta: {} no valor: {}", 
                       request.getAccountId(), request.getAmount());
            
            Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
            
            BigDecimal previousBalance = account.getBalance();
            BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
            
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                metricsService.incrementTransactionFailure();
                metricsService.recordTransactionTime(sample);
                
                auditService.auditTransaction("DEBIT", request.getAccountId(), request.getAmount(), 
                                            false, "Saldo insuficiente", correlationId);
                
                logger.warn("Débito recusado por saldo insuficiente. Conta: {}, Saldo atual: {}, Valor solicitado: {}", 
                           request.getAccountId(), previousBalance, request.getAmount());
                
                return new TransactionResponse(Status.RECUSADO, "Saldo insuficiente");
            }
            
            account.setBalance(newBalance);
            accountRepository.save(account);
            
            // Registrar métricas
            metricsService.incrementDebitOperation();
            metricsService.incrementTransactionSuccess();
            metricsService.recordTransactionTime(sample);
            
            // Auditoria
            auditService.auditTransaction("DEBIT", request.getAccountId(), request.getAmount(), 
                                        true, String.format("Saldo anterior: %s, Novo saldo: %s", 
                                        previousBalance, newBalance), correlationId);
            
            logger.info("Débito efetuado com sucesso. Conta: {}, Novo saldo: {}", 
                       request.getAccountId(), newBalance);
            
            return new TransactionResponse(Status.EFETUADO, "Débito efetuado com sucesso");
            
        } catch (Exception e) {
            metricsService.incrementTransactionFailure();
            metricsService.recordTransactionTime(sample);
            
            auditService.auditTransaction("DEBIT", request.getAccountId(), request.getAmount(), 
                                        false, "Erro: " + e.getMessage(), correlationId);
            
            logger.error("Erro ao processar débito: {}", e.getMessage(), e);
            
            return new TransactionResponse(Status.RECUSADO, "Erro ao processar débito: " + e.getMessage());
        } finally {
            MDC.clear();
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