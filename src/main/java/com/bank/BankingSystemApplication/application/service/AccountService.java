package com.bank.BankingSystemApplication.application.service;

import com.bank.BankingSystemApplication.infrastructure.audit.BankingAuditService;
import com.bank.BankingSystemApplication.domain.model.AccountCreationRequest;
import com.bank.BankingSystemApplication.domain.model.Status;
import com.bank.BankingSystemApplication.domain.model.TransactionRequest;
import com.bank.BankingSystemApplication.domain.model.TransactionResponse;
import com.bank.BankingSystemApplication.domain.model.Account;
import com.bank.BankingSystemApplication.infrastructure.monitoring.BankingMetricsService;
import com.bank.BankingSystemApplication.infrastructure.persistence.AccountRepository;
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

/**
 * Serviço principal para operações bancárias.
 * 
 * Esta classe implementa a lógica de negócio para:
 * - Criação de contas bancárias
 * - Operações de crédito e débito
 * - Validações de regras de negócio
 * - Logging e auditoria completos
 * - Métricas de performance e observabilidade
 * 
 * Características importantes:
 * - Transações atômicas com @Transactional
 * - Controle de concorrência com lock pessimista
 * - Logging estruturado com MDC (Mapped Diagnostic Context)
 * - Métricas de tempo de execução e contadores
 * - Auditoria completa de todas as operações
 * - Validação de idade mínima (18 anos)
 * - Verificação de saldo antes de débitos
 * 
 * @author Sistema Bancário
 * @version 1.0
 * @since 1.0
 */
@Service
public class AccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private BankingMetricsService metricsService;
    
    @Autowired
    private BankingAuditService auditService;
    
    /**
     * Cria uma nova conta bancária no sistema.
     * 
     * Validações realizadas:
     * - Idade mínima de 18 anos
     * - CPF único no sistema
     * - Dados obrigatórios preenchidos
     * 
     * @param request Dados para criação da conta
     * @return Account Conta criada com ID gerado
     * @throws IllegalArgumentException Se CPF já existe ou idade < 18 anos
     */
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
    
    /**
     * Realiza operação de crédito em uma conta.
     * 
     * Adiciona o valor especificado ao saldo da conta.
     * Operação sempre aprovada se a conta existir.
     * 
     * @param request Dados da transação (ID da conta e valor)
     * @return TransactionResponse Status da operação (EFETUADO/RECUSADO)
     * @throws IllegalArgumentException Se a conta não for encontrada
     */
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
    
    /**
     * Realiza operação de débito em uma conta.
     * 
     * Subtrai o valor especificado do saldo da conta.
     * Operação recusada se saldo insuficiente.
     * 
     * @param request Dados da transação (ID da conta e valor)
     * @return TransactionResponse Status da operação (EFETUADO/RECUSADO)
     * @throws IllegalArgumentException Se a conta não for encontrada
     */
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
    
    /**
     * Valida se o titular tem idade mínima para abrir conta.
     * 
     * @param birthDate Data de nascimento do titular
     * @throws IllegalArgumentException Se idade < 18 anos
     */
    private void validateAge(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("Idade mínima de 18 anos não atendida");
        }
    }
    
    /**
     * Busca uma conta pelo ID.
     * 
     * @param id ID da conta
     * @return Account Dados da conta
     * @throws IllegalArgumentException Se a conta não for encontrada
     */
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }
}