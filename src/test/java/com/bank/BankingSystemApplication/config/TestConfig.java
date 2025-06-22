package com.bank.BankingSystemApplication.config;

import com.bank.BankingSystemApplication.infrastructure.monitoring.PerformanceMetricsService;
import com.bank.BankingSystemApplication.infrastructure.monitoring.SystemLoadMonitor;
import com.bank.BankingSystemApplication.domain.port.in.BankingUseCase;
import com.bank.BankingSystemApplication.domain.port.out.AccountPersistencePort;
import com.bank.BankingSystemApplication.domain.port.out.EventPublishingPort;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public PerformanceMetricsService mockPerformanceMetricsService() {
        return Mockito.mock(PerformanceMetricsService.class);
    }
    
    @Bean
    @Primary
    public SystemLoadMonitor mockSystemLoadMonitor() {
        return Mockito.mock(SystemLoadMonitor.class);
    }
    
    @Bean
    @Primary
    public BankingUseCase mockBankingUseCase() {
        return Mockito.mock(BankingUseCase.class);
    }
    
    @Bean
    @Primary
    public AccountPersistencePort mockAccountPersistencePort() {
        return Mockito.mock(AccountPersistencePort.class);
    }
    
    @Bean
    @Primary
    public EventPublishingPort mockEventPublishingPort() {
        return Mockito.mock(EventPublishingPort.class);
    }
}