# Patterns Implementados no Sistema Bancário

Este documento descreve todos os patterns de resiliência e arquitetura implementados no sistema bancário para melhorar sua robustez, escalabilidade e manutenibilidade.

## 🔄 Patterns de Resiliência

### 1. Circuit Breaker
**Localização:** `ResilientAccountService`, `TransactionEventProducer`
**Configuração:** `application.properties` - `resilience4j.circuitbreaker.*`

**Benefícios:**
- Previne falhas em cascata
- Protege o sistema de dependências que falham
- Failover rápido quando serviços estão indisponíveis

**Configuração:**
```properties
resilience4j.circuitbreaker.instances.banking-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.banking-service.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.banking-service.sliding-window-size=10
```

**Uso:**
```java
@CircuitBreaker(name = "banking-service", fallbackMethod = "fallbackCreateAccount")
public CompletableFuture<Account> createAccountResilient(AccountCreationRequest request)
```

### 2. Retry com Backoff e Jitter
**Localização:** `TransactionEventProducer`, `ResilientAccountService`
**Configuração:** `application.properties` - `resilience4j.retry.*`

**Benefícios:**
- Recuperação automática de falhas temporárias
- Reduz impacto de problemas de rede transitórios
- Backoff exponencial evita thundering herd

**Configuração:**
```properties
resilience4j.retry.instances.banking-service.max-attempts=3
resilience4j.retry.instances.banking-service.wait-duration=500ms
```

**Uso:**
```java
@Retry(name = "banking-service", fallbackMethod = "fallbackPublishTransactionEvent")
public CompletableFuture<SendResult<String, Object>> publishTransactionEvent(TransactionEvent event)
```

### 3. Rate Limiting
**Localização:** `CQRSAccountController`, `ResilientAccountService`
**Configuração:** `application.properties` - `resilience4j.ratelimiter.*`

**Benefícios:**
- Protege contra sobrecarga do sistema
- Garante fair usage entre clientes
- Previne ataques de DDoS

**Configuração:**
```properties
resilience4j.ratelimiter.instances.banking-api.limit-for-period=100
resilience4j.ratelimiter.instances.banking-api.limit-refresh-period=1m
```

**Uso:**
```java
@RateLimiter(name = "banking-api")
public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountCreationRequest request)
```

### 4. Timeout/Time Limiter
**Localização:** `ResilientAccountService`
**Configuração:** `application.properties` - `resilience4j.timelimiter.*`

**Benefícios:**
- Evita bloqueio indefinido de threads
- Libera recursos rapidamente
- Melhora responsividade do sistema

**Configuração:**
```properties
resilience4j.timelimiter.instances.banking-service.timeout-duration=5s
resilience4j.timelimiter.instances.banking-service.cancel-running-future=true
```

**Uso:**
```java
@TimeLimiter(name = "banking-service")
public CompletableFuture<Account> createAccountResilient(AccountCreationRequest request)
```

## 🏗️ Patterns Arquiteturais

### 5. CQRS (Command Query Responsibility Segregation)
**Localização:** `cqrs/` package, `CQRSAccountController`

**Benefícios:**
- Separação clara entre operações de leitura e escrita
- Otimização independente de queries e commands
- Melhor escalabilidade e manutenibilidade

**Estrutura:**
```
cqrs/
├── command/
│   ├── CreateAccountCommand.java
│   ├── CreditCommand.java
│   └── DebitCommand.java
├── query/
│   └── AccountQuery.java
└── handler/
    ├── AccountCommandHandler.java
    └── AccountQueryHandler.java
```

**Uso:**
```java
// Command
CreateAccountCommand command = new CreateAccountCommand(request);
CompletableFuture<Account> result = commandHandler.handle(command);

// Query
AccountQuery query = new AccountQuery(accountId);
Account account = queryHandler.handle(query);
```

### 6. Saga Pattern
**Localização:** `saga/TransferSaga.java`

**Benefícios:**
- Gerencia transações distribuídas
- Garante consistência eventual
- Compensação automática em caso de falha

**Uso:**
```java
SagaResult result = transferSaga.executeTransfer(fromAccountId, toAccountId, amount);
```

**Fluxo da Saga:**
1. **Débito da conta origem**
2. **Crédito da conta destino**
3. **Compensação automática** se etapa 2 falhar

### 7. Twelve-Factor App
**Localização:** `application.properties`, `application-production.properties`

**Fatores Implementados:**

#### I. Codebase
✅ Código versionado em Git com múltiplos deploys

#### II. Dependencies
✅ Dependências explícitas via Maven (`pom.xml`)

#### III. Config
✅ Configuração via variáveis de ambiente:
```properties
server.port=${SERVER_PORT:8080}
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/banking_system}
spring.kafka.bootstrap-servers=${KAFKA_SERVERS:localhost:9092}
```

#### IV. Backing Services
✅ Serviços tratados como recursos anexados:
- PostgreSQL como serviço de dados
- Kafka como serviço de mensageria

#### V. Build, Release, Run
✅ Separação estrita via Maven e profiles

#### VI. Processes
✅ Aplicação stateless

#### VII. Port Binding
✅ Serviços exportados via binding de porta:
```properties
server.port=${SERVER_PORT:8080}
management.server.port=${MANAGEMENT_PORT:8081}
```

#### VIII. Concurrency
✅ Scaling via configuração de threads:
```properties
server.tomcat.threads.max=${MAX_THREADS:200}
server.tomcat.threads.min-spare=${MIN_THREADS:10}
```

#### IX. Disposability
✅ Startup rápido e shutdown graceful:
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

#### X. Dev/Prod Parity
✅ Ambientes similares via profiles

#### XI. Logs
✅ Logs como streams de eventos:
```properties
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

#### XII. Admin Processes
✅ Processos administrativos via Actuator:
```properties
management.endpoints.web.exposure.include=health,info,metrics,circuitbreakers,ratelimiters,retries
```

## 📊 Observabilidade

### Health Checks Customizados
**Localização:** `ObservabilityController`

**Endpoints:**
- `/api/observability/health/banking` - Health específico do sistema bancário
- `/api/observability/circuit-breakers` - Status dos circuit breakers
- `/api/observability/rate-limiters` - Status dos rate limiters
- `/api/observability/retries` - Métricas de retry
- `/api/observability/metrics/summary` - Resumo de métricas

### Métricas Disponíveis
- **Circuit Breaker:** Estado, taxa de falha, número de chamadas
- **Rate Limiter:** Permissões disponíveis, threads aguardando
- **Retry:** Tentativas com/sem retry, sucessos/falhas
- **Actuator:** Health, info, metrics integrados

## 🚀 Como Usar

### 1. Endpoints Resilientes
Use os endpoints em `/api/v2/accounts/` que implementam CQRS:
```bash
# Criar conta com CQRS
POST /api/v2/accounts

# Transferência com Saga
POST /api/v2/accounts/transfer?fromAccountId=1&toAccountId=2&amount=100.00
```

### 2. Monitoramento
Acesse os endpoints de observabilidade:
```bash
# Health check bancário
GET /api/observability/health/banking

# Métricas resumidas
GET /api/observability/metrics/summary

# Status de circuit breakers
GET /api/observability/circuit-breakers
```

### 3. Configuração de Ambiente
Configure via variáveis de ambiente:
```bash
export DATABASE_URL=jdbc:postgresql://prod-db:5432/banking
export KAFKA_SERVERS=prod-kafka:9092
export MAX_THREADS=400
export LOG_LEVEL=WARN
```

## 🔧 Configuração por Ambiente

### Desenvolvimento
```bash
export SPRING_PROFILES_ACTIVE=default
export LOG_LEVEL=DEBUG
export MAX_THREADS=50
```

### Produção
```bash
export SPRING_PROFILES_ACTIVE=production
export DATABASE_URL=jdbc:postgresql://prod-db:5432/banking
export SSL_ENABLED=true
export LOG_LEVEL=WARN
export MAX_THREADS=400
```

## 📈 Benefícios Implementados

1. **Resiliência:** Circuit breakers, retries e timeouts protegem contra falhas
2. **Escalabilidade:** Rate limiting e configuração de threads otimizam performance
3. **Manutenibilidade:** CQRS separa responsabilidades claramente
4. **Consistência:** Saga pattern garante transações distribuídas confiáveis
5. **Observabilidade:** Métricas detalhadas e health checks customizados
6. **Deploy:** Twelve-factor app facilita deploys e operações
7. **Recuperação:** Compensação automática em transações falhadas

Este conjunto de patterns transforma o sistema bancário em uma aplicação robusta, escalável e pronta para produção em ambientes distribuídos.