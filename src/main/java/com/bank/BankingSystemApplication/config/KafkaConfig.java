package com.bank.BankingSystemApplication.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    public static final String TRANSACTION_TOPIC = "banking-transactions";
    public static final String NOTIFICATION_TOPIC = "banking-notifications";
    public static final String AUDIT_TOPIC = "banking-audit";
    
    @Bean
    public NewTopic transactionTopic() {
        return TopicBuilder.name(TRANSACTION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(NOTIFICATION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(AUDIT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}