# Kafka Integration - Banking System

### Tópicos
1. **banking-transactions**: Eventos de transações financeiras
2. **banking-notifications**: Eventos para notificações aos clientes
3. **banking-audit**: Eventos para compliance e auditoria

### Componentes

#### Producers
- **TransactionEventProducer**: Publica eventos para os três tópicos
- Localização: `src/main/java/.../service/kafka/TransactionEventProducer.java`

#### Consumers
- **TransactionEventConsumer**: Processa eventos de transações e auditoria
- **NotificationEventConsumer**: Processa notificações por email
- Localização: `src/main/java/.../service/kafka/`

## 📋 APIs Disponíveis

### API Síncrona (Original)
**Base URL**: `/api/accounts`
- Processamento imediato
- Resposta síncrona
- Sem eventos Kafka

### API Assíncrona (Nova)
**Base URL**: `/api/accounts/async`
- Processamento imediato + eventos assíncronos
- Resposta síncrona da operação principal
- Eventos Kafka para auditoria e notificações

## 🔄 Fluxo de Eventos

### 1. Criação de Conta Assíncrona
```
POST /api/accounts/async
↓
1. Criar conta (síncrono)
2. Publicar evento de auditoria
3. Publicar evento de notificação (se email presente)
4. Retornar resposta
```

**Eventos Gerados:**
- `banking-audit`: Log da criação da conta
- `banking-notifications`: Email de boas-vindas

### 2. Transação de Crédito Assíncrona
```
POST /api/accounts/async/credit
↓
1. Executar crédito (síncrono)
2. Publicar evento de transação
3. Publicar evento de auditoria
4. Publicar evento de notificação
5. Retornar resposta
```

**Eventos Gerados:**
- `banking-transactions`: Detalhes da transação
- `banking-audit`: Log para compliance
- `banking-notifications`: Confirmação por email

### 3. Transação de Débito Assíncrona
```
POST /api/accounts/async/debit
↓
1. Executar débito (síncrono)
2. Publicar evento de transação
3. Publicar evento de auditoria
4. Publicar evento de notificação (sucesso/falha)
5. Retornar resposta
```

## 📦 Estrutura dos Eventos

### TransactionEvent
```json
{
  "eventId": "uuid",
  "accountId": 123,
  "amount": 100.50,
  "type": "CREDIT|DEBIT|ACCOUNT_CREATION",
  "status": "EFETUADO|RECUSADO",
  "message": "Descrição da operação",
  "timestamp": "2025-01-19T10:30:00"
}
```

### NotificationEvent
```json
{
  "eventId": "uuid",
  "accountId": 123,
  "email": "cliente@email.com",
  "message": "Mensagem para o cliente",
  "type": "ACCOUNT_CREATED|TRANSACTION_SUCCESS|TRANSACTION_FAILED|BALANCE_LOW",
  "timestamp": "2025-01-19T10:30:00"
}
```

## 🛠️ Configuração

### Configurações Kafka (application.properties)
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=banking-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.bank.BankingSystemApplication.dto
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

### Configuração de Tópicos (KafkaConfig.java)
```java
@Configuration
public class KafkaConfig {
    public static final String TRANSACTION_TOPIC = "banking-transactions";
    public static final String NOTIFICATION_TOPIC = "banking-notifications";
    public static final String AUDIT_TOPIC = "banking-audit";
    
    // Beans para criação automática dos tópicos
}
```

## 🚀 Como Usar

### 1. Subir Infraestrutura
```bash
docker-compose up -d
```

### 2. Verificar Tópicos
Acesse http://localhost:8082 (Kafka UI) para visualizar:
- Tópicos criados
- Mensagens em tempo real
- Status dos consumers

### 3. Testar API Assíncrona
```bash
# Criar conta assíncrona
curl -X POST http://localhost:8080/api/accounts/async \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Kafka User",
    "cpf": "55566677788",
    "birthDate": "1990-01-01",
    "email": "kafka@email.com"
  }'

# Creditar conta
curl -X POST http://localhost:8080/api/accounts/async/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 500.00
  }'
```

### 4. Monitorar Eventos
No Kafka UI (http://localhost:8082):
1. Acesse "Topics"
2. Clique em "banking-transactions", "banking-notifications" ou "banking-audit"
3. Visualize as mensagens em tempo real

## 📊 Casos de Uso

### 1. Auditoria e Compliance
- **Problema**: Necessidade de rastrear todas as operações
- **Solução**: Tópico `banking-audit` com log detalhado
- **Benefício**: Compliance automático, relatórios em tempo real

### 2. Notificações Automáticas
- **Problema**: Clientes precisam ser notificados de transações
- **Solução**: Tópico `banking-notifications` com diferentes tipos
- **Benefício**: Experiência do cliente melhorada

### 3. Processamento Assíncrono
- **Problema**: Operações demoradas podem travar a API
- **Solução**: Operação principal síncrona + eventos assíncronos
- **Benefício**: API responsiva, processamento paralelo

### 4. Análise de Fraude (Futuro)
- **Implementação Futura**: Consumer adicional no tópico de transações
- **Funcionalidade**: Análise em tempo real de padrões suspeitos
- **Benefício**: Detecção proativa de fraudes

## 🔧 Troubleshooting

### Kafka não conecta
```bash
# Verificar status dos containers
docker-compose ps

# Ver logs do Kafka
docker-compose logs kafka

# Verificar se os tópicos existem
docker exec -it banking_kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Eventos não são consumidos
```bash
# Verificar consumer groups
docker exec -it banking_kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Ver lag dos consumers
docker exec -it banking_kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group banking-group
```

### Debug de mensagens
```bash
# Consumir mensagens do terminal
docker exec -it banking_kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic banking-transactions --from-beginning
```

## 🎯 Benefícios da Implementação

### Para o Negócio
- **Auditoria**: Compliance automático com rastreamento completo
- **Experiência do Cliente**: Notificações em tempo real
- **Escalabilidade**: Processamento paralelo de eventos

### Para o Desenvolvimento
- **Desacoplamento**: Serviços independentes
- **Manutenibilidade**: Lógica separada por responsabilidade
- **Testabilidade**: Eventos podem ser mockados

### Para a Operação
- **Monitoramento**: Kafka UI para visualização em tempo real
- **Observabilidade**: Logs estruturados de todos os eventos
- **Confiabilidade**: Garantia de entrega com Kafka

## 🔮 Próximos Passos

1. **Dead Letter Topics**: Para eventos que falharam
2. **Retry Policies**: Reprocessamento automático
3. **Schema Registry**: Versionamento de eventos
4. **Monitoring**: Métricas detalhadas com Prometheus
5. **Análise de Fraude**: ML em tempo real nos eventos
6. **Event Sourcing**: Histórico completo de mudanças de estado