package com.bank.BankingSystemApplication.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuração do Apache Kafka para o sistema bancário.
 * 
 * Esta classe define os tópicos Kafka utilizados para:
 * - Eventos de transações (banking-transactions)
 * - Notificações (banking-notifications)  
 * - Auditoria (banking-audit)
 * 
 * Configurações aplicadas:
 * - 3 partições por tópico para paralelismo
 * - 1 réplica (adequado para desenvolvimento/teste)
 * - Criação automática dos tópicos na inicialização
 * 
 * Os tópicos seguem o padrão de nomenclatura 'banking-*' 
 * para identificação clara no cluster Kafka.
 * 
 * @author Sistema Bancário
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class KafkaConfig {
    
    /** Nome do tópico para eventos de transações bancárias */
    public static final String TRANSACTION_TOPIC = "banking-transactions";
    
    /** Nome do tópico para notificações do sistema */
    public static final String NOTIFICATION_TOPIC = "banking-notifications";
    
    /** Nome do tópico para eventos de auditoria */
    public static final String AUDIT_TOPIC = "banking-audit";
    
    /**
     * Configura o tópico para eventos de transações.
     * 
     * @return NewTopic configurado para transações
     */
    @Bean
    public NewTopic transactionTopic() {
        return TopicBuilder.name(TRANSACTION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    /**
     * Configura o tópico para notificações.
     * 
     * @return NewTopic configurado para notificações
     */
    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(NOTIFICATION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    /**
     * Configura o tópico para auditoria.
     * 
     * @return NewTopic configurado para auditoria
     */
    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(AUDIT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}