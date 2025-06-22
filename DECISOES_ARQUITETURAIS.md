# 🏗️ Banking System - Decisões Arquiteturais

## 📋 Índice
1. [Visão Geral da Arquitetura](#visão-geral-da-arquitetura)
2. [Decisões de Banco de Dados](#decisões-de-banco-de-dados)
3. [Message Broker](#message-broker)
4. [Patterns Arquiteturais](#patterns-arquiteturais)
5. [Framework e Tecnologias](#framework-e-tecnologias)
6. [Observabilidade](#observabilidade)
7. [Resiliência](#resiliência)

---

## 🎯 Visão Geral da Arquitetura

### Arquitetura Hexagonal (Ports & Adapters)
**Decisão**: Implementação da arquitetura hexagonal com API Gateway inteligente

**Motivação**:
- **Isolamento do domínio**: Lógica de negócio independente de frameworks
- **Testabilidade**: Facilita testes unitários e de integração
- **Flexibilidade**: Permite trocar implementações sem afetar o core
- **Manutenibilidade**: Separação clara de responsabilidades

**Estrutura Implementada**:
```
domain/
├── port/in/          # Casos de uso (interfaces)
├── port/out/         # Portas para infraestrutura
└── service/          # Lógica de negócio

adapter/
├── in/
│   ├── web/          # Controllers REST
│   └── messaging/    # Consumers Kafka
└── out/
    ├── persistence/  # Implementação do banco
    └── messaging/    # Producers Kafka
```

**Benefícios Realizados**:
- Fácil substituição de implementações
- Testes isolados do domínio
- Deploy independente de camadas
- Evolução arquitetural controlada

---

## 🗄️ Decisões de Banco de Dados

### PostgreSQL
**Decisão**: PostgreSQL como banco principal

**Motivação**:
1. **ACID Compliance**: Transações bancárias exigem consistência absoluta
2. **Performance**: Otimizado para operações transacionais complexas
3. **JSON Support**: Flexibilidade para armazenar metadados
4. **Ecosystem**: Tooling maduro e documentação extensa
5. **Concorrência**: Controle de versão otimista com `@Version`

**Configuração Escolhida**:
```properties
# Otimizações para transações bancárias
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
spring.flyway.enabled=true
```

**Alternativas Consideradas**:
- **MySQL**: Descartado por menor suporte a transações complexas
- **Oracle**: Descartado por custo e complexidade
- **MongoDB**: Descartado por ser NoSQL (inconsistente para banking)

### Controle de Concorrência
**Decisão**: Versionamento otimista com `@Version`

**Implementação**:
```java
@Entity
public class Account {
    @Version
    private Long version;
    // Evita dirty reads e lost updates
}
```

**Motivação**:
- Melhor performance que locks pessimistas
- Menor chance de deadlocks
- Escalabilidade horizontal

---

## 📨 Message Broker

### Apache Kafka
**Decisão**: Kafka como message broker principal

**Motivação**:
1. **Event Streaming**: Ideal para event-driven architecture
2. **Durabilidade**: Persiste mensagens para auditoria
3. **Throughput**: Alta performance para grandes volumes
4. **Ordenação**: Garante ordem de mensagens por partição
5. **Replay**: Capacidade de reprocessar eventos

**Tópicos Implementados**:
```yaml
Tópicos Bancários:
- banking.account.create     # Solicitações de criação
- banking.account.created    # Contas criadas com sucesso  
- banking.account.failed     # Falhas na criação
- banking.transaction.credit # Operações de crédito
- banking.transaction.debit  # Operações de débito
- banking.transaction.processed # Transações processadas
- banking.notifications      # Notificações aos usuários
```

**Configuração**:
```properties
# Configuração para consistência
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.consumer.auto-offset-reset=earliest
```

**Alternativas Consideradas**:
- **RabbitMQ**: Descartado por menor throughput
- **Amazon SQS**: Descartado por vendor lock-in
- **Redis Streams**: Descartado por menor durabilidade

### Event Sourcing Parcial
**Decisão**: Event sourcing para auditoria, CRUD para queries

**Implementação**:
- Comandos geram eventos no Kafka
- Estado atual mantido no PostgreSQL
- Histórico completo preservado nos eventos

---

## 🏛️ Patterns Arquiteturais

### 1. API Gateway Pattern
**Decisão**: Gateway inteligente com roteamento baseado em carga

**Implementação**:
```java
@RestController
public class ApiGateway {
    public ResponseEntity<?> createAccount(AccountCreationRequest request) {
        if (loadMonitor.shouldUseAsyncProcessing()) {
            return asyncAdapter.createAccountAsync(request);
        } else {
            return syncController.createAccount(request);
        }
    }
}
```

**Motivação**:
- **Load Balancing**: Distribui carga entre sync/async
- **Circuit Breaker**: Proteção contra falhas em cascata
- **Monitoring**: Ponto único para observabilidade
- **Rate Limiting**: Controle de tráfego centralizado

### 2. CQRS (Command Query Responsibility Segregation)
**Decisão**: CQRS para separar comandos de queries

**Implementação**:
```java
// Commands
@Component
public class AccountCommandHandler {
    public void handle(CreateAccountCommand cmd) { ... }
}

// Queries  
@Component
public class AccountQueryHandler {
    public Account handle(AccountQuery query) { ... }
}
```

**Motivação**:
- **Escalabilidade**: Otimizar reads e writes independentemente
- **Simplicidade**: Modelos específicos para cada operação
- **Performance**: Queries otimizadas sem impactar comandos

### 3. Saga Pattern
**Decisão**: Orchestration Saga para transações distribuídas

**Implementação**:
```java
@Component
public class TransferSaga {
    @SagaStart
    public void handle(TransferCommand command) {
        // Orchestração de múltiplos serviços
    }
}
```

**Motivação**:
- **Transações Distribuídas**: Sem 2PC
- **Compensação**: Rollback automático em falhas
- **Visibilidade**: Estado da transação trackeável

### 4. Decorator Pattern
**Decisão**: Decorators para cross-cutting concerns

**Implementação**:
```java
// Resilience Decorator
public class ResilientAccountService implements AccountService {
    @CircuitBreaker(name = "banking-service")
    @Retry(name = "banking-service")
    public Account createAccount(AccountCreationRequest request) {
        return delegate.createAccount(request);
    }
}

// Async Decorator
public class AsyncAccountService implements AccountService {
    public Account createAccount(AccountCreationRequest request) {
        Account account = delegate.createAccount(request);
        eventPublisher.publishAccountCreated(account);
        return account;
    }
}
```

**Motivação**:
- **Single Responsibility**: Cada decorator tem uma responsabilidade
- **Composição**: Facilita combinação de comportamentos
- **Testabilidade**: Testagem isolada de cada concern

---

## 🚀 Framework e Tecnologias

### Spring Boot 3.1.5
**Decisão**: Spring Boot como framework principal

**Motivação**:
1. **Maturidade**: Framework consolidado para enterprise
2. **Ecosystem**: Integração nativa com Kafka, PostgreSQL, etc.
3. **Jakarta EE**: Migração para padrões modernos
4. **Observabilidade**: Actuator para monitoramento
5. **Cloud Native**: Preparado para containers e Kubernetes

**Configuração**:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
</parent>
```

### Java 17
**Decisão**: Java 17 LTS

**Motivação**:
- **LTS**: Suporte de longo prazo até 2029
- **Performance**: Melhorias significativas de GC
- **Records**: Simplifica DTOs e Value Objects
- **Pattern Matching**: Código mais limpo e legível

**Exemplos de Uso**:
```java
// Records para DTOs
public record AccountCreationRequest(
    String name,
    String cpf,
    LocalDate birthDate
) {}

// Pattern matching
switch (status) {
    case SUCCESS -> processSuccess();
    case FAILURE -> processFailure();
}
```

### Resilience4j
**Decisão**: Resilience4j para patterns de resiliência

**Motivação**:
- **Spring Boot 3**: Compatibilidade nativa
- **Lightweight**: Menor overhead que Hystrix
- **Composição**: Combina múltiplos patterns facilmente

**Configuração Implementada**:
```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.banking-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.banking-service.wait-duration-in-open-state=30s

# Retry
resilience4j.retry.instances.banking-service.max-attempts=3
resilience4j.retry.instances.banking-service.wait-duration=500ms

# Rate Limiter
resilience4j.ratelimiter.instances.banking-api.limit-for-period=100
resilience4j.ratelimiter.instances.banking-api.limit-refresh-period=1m
```

---

## 📊 Observabilidade

### Arquitetura de Observabilidade
**Decisão**: Three Pillars of Observability (Metrics, Logs, Traces)

**Stack Implementada**:
1. **Metrics**: Micrometer + Prometheus
2. **Logs**: Logback + Structured JSON
3. **Traces**: Zipkin + Spring Cloud Sleuth

### Métricas Customizadas
**Decisão**: Métricas específicas para banking

**Implementação**:
```java
@Service
public class PerformanceMetricsService {
    private final Counter totalRequestsCounter;
    private final Timer requestDurationTimer;
    private final Gauge activeConnectionsGauge;
    
    // Métricas de negócio
    public void recordTransaction(TransactionType type, BigDecimal amount) {
        transactionCounter.increment(
            Tags.of("type", type.toString(), "amount_range", getAmountRange(amount))
        );
    }
}
```

**Métricas Implementadas**:
- **TPS**: Transações por segundo
- **Latência**: P50, P95, P99 de response time
- **Error Rate**: Taxa de erro por endpoint
- **Business**: Volume transacionado, contas criadas
- **Infrastructure**: CPU, memória, conexões DB

### Distributed Tracing
**Decisão**: Zipkin para correlação de requests

**Configuração**:
```properties
management.tracing.enabled=true
management.tracing.sampling.probability=0.1
management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans
```

**Benefícios**:
- Trackeamento end-to-end de requests
- Identificação de gargalos entre serviços
- Análise de latência por componente

---

## 🛡️ Resiliência

### Strategy de Resiliência
**Decisão**: Defense in Depth com múltiplas camadas

**Camadas Implementadas**:

1. **API Gateway**: Rate limiting e circuit breaking
2. **Service Layer**: Retry e timeout
3. **Database**: Connection pooling e query timeout
4. **Messaging**: Dead letter queues e retry policies

### Circuit Breaker Strategy
**Decisão**: Circuit breaker adaptativo

**Configuração**:
```properties
# Estados do circuit breaker
- CLOSED: Operação normal (failure rate < 50%)
- OPEN: Falha rápida (failure rate >= 50%) por 30s
- HALF_OPEN: Teste gradual de recuperação
```

**Implementação**:
```java
@CircuitBreaker(name = "banking-service", fallbackMethod = "fallbackCreateAccount")
@Retry(name = "banking-service")
@TimeLimiter(name = "banking-service")
public CompletableFuture<Account> createAccountAsync(AccountCreationRequest request) {
    return CompletableFuture.supplyAsync(() -> createAccount(request));
}

public CompletableFuture<Account> fallbackCreateAccount(AccountCreationRequest request, Exception e) {
    // Fallback para modo degradado
    return CompletableFuture.completedFuture(createAccountOffline(request));
}
```

### Graceful Degradation
**Decisão**: Modo degradado para alta disponibilidade

**Estratégias**:
- **Read-only mode**: Quando write operations falham
- **Async fallback**: Queue requests quando sync falha
- **Cache fallback**: Serve dados cached quando DB falha

---

## 🔄 Versionamento e Deployment

### API Versioning
**Decisão**: URL versioning para backward compatibility

**Estratégia**:
```
/api/v1/accounts  # Versão legacy
/api/v2/accounts  # Versão CQRS
/api/gateway/*    # Gateway inteligente (último)
```

### Feature Flags
**Decisão**: Configuration-based feature toggles

**Implementação**:
```properties
# Feature flags
performance.monitoring.enabled=true
async.processing.enabled=true
cqrs.enabled=true
```

### Blue-Green Deployment
**Decisão**: Preparação para deployment sem downtime

**Configuração Docker**:
```yaml
# Health checks para blue-green
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
```

---

## 📈 Performance e Scalability

### Load-based Routing
**Decisão**: Roteamento inteligente baseado em métricas do sistema

**Implementação**:
```java
public boolean shouldUseAsyncProcessing() {
    double currentCpu = getCurrentCpuUsage();
    int currentConnections = getActiveConnections();
    
    return currentCpu > cpuThreshold || currentConnections > connectionThreshold;
}
```

**Thresholds Configurados**:
- **CPU Threshold**: 70%
- **Connection Threshold**: 100 conexões ativas
- **Response Time Threshold**: 500ms

### Caching Strategy
**Decisão**: Multi-level caching

**Camadas**:
1. **Application Cache**: Queries frequentes (accounts lookup)
2. **HTTP Cache**: Headers para CDN
3. **Database Cache**: Query plan caching

---

## 🔐 Segurança

### Princípios de Segurança
**Decisões implementadas**:

1. **Defense in Depth**: Múltiplas camadas de segurança
2. **Principle of Least Privilege**: Permissões mínimas necessárias
3. **Fail Secure**: Falha em estado seguro

### Implementações
- **Input Validation**: Bean Validation em todos DTOs
- **SQL Injection Prevention**: JPA prepared statements
- **Correlation IDs**: Tracking de requests para auditoria
- **Structured Logging**: Logs estruturados para SIEM

---

## 🎯 Conclusão

### Decisões Críticas Tomadas

1. **Arquitetura Hexagonal**: Isolamento e testabilidade
2. **PostgreSQL**: Consistência e ACID para banking
3. **Kafka**: Event streaming e auditoria
4. **Spring Boot 3**: Ecosystem maduro e cloud-native
5. **Observabilidade**: Three pillars implementados
6. **Resiliência**: Defense in depth strategy

### Trade-offs Aceitos

1. **Complexidade vs Flexibilidade**: Arquitetura mais complexa para maior flexibilidade
2. **Performance vs Consistency**: Priorização de consistência sobre performance extrema
3. **Storage vs Auditoria**: Overhead de storage para auditoria completa

### Próximos Passos

1. **Kubernetes**: Migration para orquestração de containers
2. **Event Sourcing**: Expansão para event sourcing completo
3. **CQRS Read Models**: Implementação de read models otimizados
4. **Machine Learning**: Detecção de fraudes com ML