package com.bank.BankingSystemApplication.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de métricas customizadas para observabilidade do sistema bancário.
 * 
 * Esta classe coleta e expõe métricas de performance e negócio usando Micrometer:
 * 
 * Contadores (Counters):
 * - Contas criadas
 * - Transações executadas (sucesso/falha)
 * - Operações de crédito/débito
 * - Transferências e status de Sagas
 * 
 * Temporizadores (Timers):
 * - Tempo de criação de contas
 * - Tempo de processamento de transações
 * - Tempo de publicação no Kafka
 * - Tempo de execução de Sagas
 * 
 * Medidores (Gauges):
 * - Contas ativas no momento
 * - Saldo total do sistema
 * - Transações ativas
 * - Sagas em execução
 * 
 * As métricas são automaticamente expostas via Actuator/Prometheus
 * para monitoramento com ferramentas como Grafana.
 * 
 * @author Sistema Bancário
 * @version 1.0
 * @since 1.0
 */
@Service
public class BankingMetricsService {
    
    // Contadores
    private final Counter accountCreatedCounter;
    private final Counter transactionSuccessCounter;
    private final Counter transactionFailureCounter;
    private final Counter creditOperationsCounter;
    private final Counter debitOperationsCounter;
    private final Counter transferInitiatedCounter;
    private final Counter transferCompletedCounter;
    private final Counter transferFailedCounter;
    private final Counter sagaCompensatedCounter;
    
    // Timers para latência
    private final Timer accountCreationTimer;
    private final Timer transactionProcessingTimer;
    private final Timer kafkaPublishTimer;
    private final Timer sagaExecutionTimer;
    
    // Gauges para estado atual
    private final AtomicLong activeAccountsGauge = new AtomicLong(0);
    private final AtomicLong totalBalanceGauge = new AtomicLong(0);
    private final AtomicLong activeTransactionsGauge = new AtomicLong(0);
    private final AtomicLong activeSagasGauge = new AtomicLong(0);
    
    @Autowired
    public BankingMetricsService(MeterRegistry meterRegistry) {
        // Inicializar contadores
        this.accountCreatedCounter = Counter.builder("banking.accounts.created")
                .description("Número total de contas criadas")
                .register(meterRegistry);
                
        this.transactionSuccessCounter = Counter.builder("banking.transactions.success")
                .description("Número total de transações bem-sucedidas")
                .register(meterRegistry);
                
        this.transactionFailureCounter = Counter.builder("banking.transactions.failure")
                .description("Número total de transações falhadas")
                .register(meterRegistry);
                
        this.creditOperationsCounter = Counter.builder("banking.operations.credit")
                .description("Número total de operações de crédito")
                .register(meterRegistry);
                
        this.debitOperationsCounter = Counter.builder("banking.operations.debit")
                .description("Número total de operações de débito")
                .register(meterRegistry);
                
        this.transferInitiatedCounter = Counter.builder("banking.transfers.initiated")
                .description("Número total de transferências iniciadas")
                .register(meterRegistry);
                
        this.transferCompletedCounter = Counter.builder("banking.transfers.completed")
                .description("Número total de transferências completadas")
                .register(meterRegistry);
                
        this.transferFailedCounter = Counter.builder("banking.transfers.failed")
                .description("Número total de transferências falhadas")
                .register(meterRegistry);
                
        this.sagaCompensatedCounter = Counter.builder("banking.saga.compensated")
                .description("Número total de sagas compensadas")
                .register(meterRegistry);
        
        // Inicializar timers
        this.accountCreationTimer = Timer.builder("banking.accounts.creation.duration")
                .description("Tempo de criação de contas")
                .register(meterRegistry);
                
        this.transactionProcessingTimer = Timer.builder("banking.transactions.processing.duration")
                .description("Tempo de processamento de transações")
                .register(meterRegistry);
                
        this.kafkaPublishTimer = Timer.builder("banking.kafka.publish.duration")
                .description("Tempo de publicação no Kafka")
                .register(meterRegistry);
                
        this.sagaExecutionTimer = Timer.builder("banking.saga.execution.duration")
                .description("Tempo de execução de sagas")
                .register(meterRegistry);
        
        // Inicializar gauges
        Gauge.builder("banking.accounts.active", activeAccountsGauge, AtomicLong::get)
                .description("Número de contas ativas")
                .register(meterRegistry);
                
        Gauge.builder("banking.balance.total", totalBalanceGauge, AtomicLong::get)
                .description("Saldo total do sistema")
                .register(meterRegistry);
                
        Gauge.builder("banking.transactions.active", activeTransactionsGauge, AtomicLong::get)
                .description("Número de transações ativas")
                .register(meterRegistry);
                
        Gauge.builder("banking.saga.active", activeSagasGauge, AtomicLong::get)
                .description("Número de sagas ativas")
                .register(meterRegistry);
    }
    
    // Métodos para incrementar contadores
    public void incrementAccountCreated() {
        accountCreatedCounter.increment();
        activeAccountsGauge.incrementAndGet();
    }
    
    public void incrementTransactionSuccess() {
        transactionSuccessCounter.increment();
    }
    
    public void incrementTransactionFailure() {
        transactionFailureCounter.increment();
    }
    
    public void incrementCreditOperation() {
        creditOperationsCounter.increment();
    }
    
    public void incrementDebitOperation() {
        debitOperationsCounter.increment();
    }
    
    public void incrementTransferInitiated() {
        transferInitiatedCounter.increment();
        activeSagasGauge.incrementAndGet();
    }
    
    public void incrementTransferCompleted() {
        transferCompletedCounter.increment();
        activeSagasGauge.decrementAndGet();
    }
    
    public void incrementTransferFailed() {
        transferFailedCounter.increment();
        activeSagasGauge.decrementAndGet();
    }
    
    public void incrementSagaCompensated() {
        sagaCompensatedCounter.increment();
    }
    
    // Métodos para timers
    public Timer.Sample startAccountCreationTimer() {
        return Timer.start();
    }
    
    public void recordAccountCreationTime(Timer.Sample sample) {
        sample.stop(accountCreationTimer);
    }
    
    public Timer.Sample startTransactionTimer() {
        activeTransactionsGauge.incrementAndGet();
        return Timer.start();
    }
    
    public void recordTransactionTime(Timer.Sample sample) {
        sample.stop(transactionProcessingTimer);
        activeTransactionsGauge.decrementAndGet();
    }
    
    public Timer.Sample startKafkaPublishTimer() {
        return Timer.start();
    }
    
    public void recordKafkaPublishTime(Timer.Sample sample) {
        sample.stop(kafkaPublishTimer);
    }
    
    public Timer.Sample startSagaTimer() {
        return Timer.start();
    }
    
    public void recordSagaTime(Timer.Sample sample) {
        sample.stop(sagaExecutionTimer);
    }
    
    // Métodos para atualizar gauges
    public void updateTotalBalance(BigDecimal newBalance) {
        totalBalanceGauge.set(newBalance.longValue());
    }
    
    public void decrementActiveAccounts() {
        activeAccountsGauge.decrementAndGet();
    }
    
    // Métodos para obter métricas atuais
    public long getAccountCreatedCount() {
        return (long) accountCreatedCounter.count();
    }
    
    public long getTransactionSuccessCount() {
        return (long) transactionSuccessCounter.count();
    }
    
    public long getTransactionFailureCount() {
        return (long) transactionFailureCounter.count();
    }
    
    public long getActiveAccountsCount() {
        return activeAccountsGauge.get();
    }
    
    public long getTotalBalance() {
        return totalBalanceGauge.get();
    }
    
    public double getTransactionSuccessRate() {
        long success = getTransactionSuccessCount();
        long failure = getTransactionFailureCount();
        long total = success + failure;
        return total > 0 ? (double) success / total * 100.0 : 0.0;
    }
}