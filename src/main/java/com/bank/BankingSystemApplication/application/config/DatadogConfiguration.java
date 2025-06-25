package com.bank.BankingSystemApplication.application.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.datadog.DatadogConfig;
import io.micrometer.datadog.DatadogMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DatadogConfiguration {

    @Value("${management.metrics.export.datadog.api-key:}")
    private String apiKey;

    @Value("${management.metrics.export.datadog.application-key:}")
    private String applicationKey;

    @Value("${management.metrics.export.datadog.enabled:false}")
    private boolean enabled;

    @Value("${spring.application.name:banking-system}")
    private String applicationName;

    @Value("${datadog.environment:local}")
    private String environment;

    @Bean
    public DatadogConfig datadogConfig() {
        return new DatadogConfig() {
            @Override
            public String apiKey() {
                return apiKey;
            }

            @Override
            public String applicationKey() {
                return applicationKey;
            }

            @Override
            public boolean enabled() {
                return enabled && !apiKey.isEmpty();
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(60);
            }

            @Override
            public String get(String key) {
                return null;
            }
        };
    }

    @Bean
    public DatadogMeterRegistry datadogMeterRegistry(DatadogConfig datadogConfig) {
        return new DatadogMeterRegistry(datadogConfig, io.micrometer.core.instrument.Clock.SYSTEM);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", applicationName,
            "environment", environment,
            "service", "banking-system",
            "version", "1.0.0"
        );
    }
}