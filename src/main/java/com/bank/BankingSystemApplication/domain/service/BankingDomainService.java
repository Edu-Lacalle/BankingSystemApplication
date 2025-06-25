package com.bank.BankingSystemApplication.domain.service;

import com.bank.BankingSystemApplication.domain.port.in.BankingUseCase;
import com.bank.BankingSystemApplication.domain.port.out.AccountPersistencePort;
import com.bank.BankingSystemApplication.domain.port.out.EventPublishingPort;
import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Status;
import com.bank.BankingSystemApplication.domain.model.TransactionEvent;
import com.bank.BankingSystemApplication.domain.model.TransactionType;
import com.bank.BankingSystemApplication.domain.model.NotificationEvent;
import com.bank.BankingSystemApplication.domain.model.NotificationType;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.infrastructure.audit.BankingAuditService;
import com.bank.BankingSystemApplication.infrastructure.monitoring.BankingMetricsService;
import com.bank.BankingSystemApplication.infrastructure.async.AsyncNotificationService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Service
public class BankingDomainService implements BankingUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(BankingDomainService.class);
    
    @Autowired
    private AccountPersistencePort persistencePort;
    
    @Autowired
    private EventPublishingPort eventPort;
    
    @Autowired
    private BankingMetricsService metricsService;
    
    @Autowired
    private BankingAuditService auditService;
    
    @Autowired
    private AsyncNotificationService asyncNotificationService;
    
    @Override
    @Transactional
    public Account createAccount(AccountCreationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "createAccount");
        
        Timer.Sample sample = metricsService.startAccountCreationTimer();
        
        try {
            logger.info("Domain: Creating account for CPF: {}", maskCpf(request.getCpf()));
            
            validateAge(request.getBirthDate());
            
            if (persistencePort.findByCpf(request.getCpf()).isPresent()) {
                auditService.auditAccountCreation(null, request.getCpf(), request.getName(), 
                                                false, "CPF já cadastrado");
                throw new IllegalArgumentException("CPF já cadastrado");
            }
        
            Account account = new Account();
            account.setName(request.getName());
            account.setCpf(request.getCpf());
            account.setBirthDate(request.getBirthDate());
            
            // Only set email if it's not null and not empty
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                account.setEmail(request.getEmail().trim());
            }
            
            // Only set phone if it's not null and not empty
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                account.setPhone(request.getPhone().trim());
            }
            
            Account savedAccount = persistencePort.save(account);
            
            metricsService.incrementAccountCreated();
            metricsService.recordAccountCreationTime(sample);
            
            auditService.auditAccountCreation(savedAccount.getId(), request.getCpf(), 
                                            request.getName(), true, "Conta criada com sucesso");
            
            logger.info("Domain: Account created successfully. ID: {}", savedAccount.getId());
            
            // Publish notification event asynchronously after transaction commits
            asyncNotificationService.publishAccountCreationNotification(savedAccount);
            
            return savedAccount;
            
        } catch (Exception e) {
            metricsService.recordAccountCreationTime(sample);
            logger.error("Domain: Error creating account: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    @Transactional
    public TransactionResponse credit(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "credit");
        
        Timer.Sample sample = metricsService.startTransactionTimer();
        
        try {
            logger.info("Domain: Processing credit for account: {} amount: {}", 
                       request.getAccountId(), request.getAmount());
            
            Account account = persistencePort.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
            
            BigDecimal previousBalance = account.getBalance();
            account.setBalance(account.getBalance().add(request.getAmount()));
            persistencePort.save(account);
            
            metricsService.incrementCreditOperation();
            metricsService.incrementTransactionSuccess();
            metricsService.recordTransactionTime(sample);
            
            auditService.auditTransaction("CREDIT", request.getAccountId(), request.getAmount(), 
                                        true, String.format("Balance: %s -> %s", 
                                        previousBalance, account.getBalance()), correlationId);
            
            publishTransactionEvent(request, account, TransactionType.CREDIT, true);
            
            logger.info("Domain: Credit processed successfully");
            
            return new TransactionResponse(Status.EFETUADO, "Crédito efetuado com sucesso");
            
        } catch (Exception e) {
            metricsService.incrementTransactionFailure();
            metricsService.recordTransactionTime(sample);
            logger.error("Domain: Error processing credit: {}", e.getMessage(), e);
            return new TransactionResponse(Status.RECUSADO, e.getMessage());
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    @Transactional
    public TransactionResponse debit(TransactionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("operation", "debit");
        
        Timer.Sample sample = metricsService.startTransactionTimer();
        
        try {
            logger.info("Domain: Processing debit for account: {} amount: {}", 
                       request.getAccountId(), request.getAmount());
            
            Account account = persistencePort.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
            
            BigDecimal previousBalance = account.getBalance();
            BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
            
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                metricsService.incrementTransactionFailure();
                metricsService.recordTransactionTime(sample);
                
                auditService.auditTransaction("DEBIT", request.getAccountId(), request.getAmount(), 
                                            false, "Saldo insuficiente", correlationId);
                
                publishTransactionEvent(request, account, TransactionType.DEBIT, false);
                
                logger.warn("Domain: Insufficient balance for debit");
                return new TransactionResponse(Status.RECUSADO, "Saldo insuficiente");
            }
            
            account.setBalance(newBalance);
            persistencePort.save(account);
            
            metricsService.incrementDebitOperation();
            metricsService.incrementTransactionSuccess();
            metricsService.recordTransactionTime(sample);
            
            auditService.auditTransaction("DEBIT", request.getAccountId(), request.getAmount(), 
                                        true, String.format("Balance: %s -> %s", 
                                        previousBalance, newBalance), correlationId);
            
            publishTransactionEvent(request, account, TransactionType.DEBIT, true);
            
            logger.info("Domain: Debit processed successfully");
            
            return new TransactionResponse(Status.EFETUADO, "Débito efetuado com sucesso");
            
        } catch (Exception e) {
            metricsService.incrementTransactionFailure();
            metricsService.recordTransactionTime(sample);
            logger.error("Domain: Error processing debit: {}", e.getMessage(), e);
            return new TransactionResponse(Status.RECUSADO, "Erro ao processar débito: " + e.getMessage());
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    public Account getAccountById(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }
    
    private void validateAge(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("Idade mínima de 18 anos não atendida");
        }
    }
    
    private void publishTransactionEvent(TransactionRequest request, Account account, 
                                       TransactionType type, boolean success) {
        TransactionEvent event = new TransactionEvent();
        event.setAccountId(request.getAccountId());
        event.setAmount(request.getAmount());
        event.setType(type);
        event.setSuccess(success);
        event.setBalance(account.getBalance());
        event.setTimestamp(LocalDateTime.now());
        
        eventPort.publishTransactionEvent(event);
    }
    
    private String maskCpf(String cpf) {
        return cpf.substring(0, 3) + "*****" + cpf.substring(8);
    }
    
}