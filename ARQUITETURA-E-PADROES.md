# Sistema Bancário - Arquitetura e Padrões Implementados

## 📋 Índice
1. [Visão Geral do Sistema](#visão-geral-do-sistema)
2. [Arquitetura Hexagonal](#arquitetura-hexagonal)
3. [Gateway Inteligente](#gateway-inteligente)
4. [Padrões de Design Implementados](#padrões-de-design-implementados)
5. [Tecnologias e Ferramentas](#tecnologias-e-ferramentas)
6. [Estrutura de Pastas](#estrutura-de-pastas)
7. [Fluxos de Operação](#fluxos-de-operação)
8. [Observabilidade e Monitoramento](#observabilidade-e-monitoramento)
9. [Configurações Principais](#configurações-principais)
10. [Como Testar](#como-testar)

---

## 🎯 Visão Geral do Sistema

Este é um **Sistema Bancário Moderno** que implementa operações bancárias básicas (criação de contas, crédito e débito) com uma arquitetura robusta, escalável e resiliente.

### Principais Características:
- ✅ **Arquitetura Hexagonal** (Ports and Adapters)
- ✅ **Gateway Inteligente** com roteamento baseado em carga
- ✅ **Processamento Síncrono e Assíncrono**
- ✅ **Event-Driven Architecture** com Kafka
- ✅ **Observabilidade Completa** (Métricas, Logs, Tracing)
- ✅ **Resiliência** (Circuit Breaker, Retry, Rate Limiting)

---

## 🏗️ Arquitetura Hexagonal

### Por que Arquitetura Hexagonal?

A **Arquitetura Hexagonal** foi escolhida para:

1. **Isolamento do Domínio**: A lógica de negócio fica completamente isolada de frameworks e tecnologias
2. **Testabilidade**: Facilita testes unitários sem dependências externas
3. **Flexibilidade**: Permite trocar adapters sem afetar o core do sistema
4. **Manutenibilidade**: Código mais limpo e organizado

### Estrutura da Arquitetura:

```
┌─────────────────────────────────────────────────┐
│                ADAPTERS (Drivers)               │
│  ┌─────────────────┐  ┌─────────────────────┐   │
│  │  API Gateway    │  │  Async Worker       │   │
│  │  REST Controller│  │  Kafka Consumer     │   │
│  └─────────────────┘  └─────────────────────┘   │
└─────────────────┬───────────────┬───────────────┘
                  │               │
           ┌──────▼───────────────▼──────┐
           │        DOMAIN CORE          │
           │  ┌─────────────────────────┐ │
           │  │   Banking Use Cases     │ │
           │  │   Business Logic        │ │
           │  │   Domain Rules          │ │
           │  └─────────────────────────┘ │
           └──────┬───────────────┬──────┘
                  │               │
┌─────────────────┴───────────────┴───────────────┐
│                ADAPTERS (Driven)                │
│  ┌─────────────────┐  ┌─────────────────────┐   │
│  │  Database       │  │  Kafka Producer     │   │
│  │  Persistence    │  │  Event Publishing   │   │
│  └─────────────────┘  └─────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### Componentes Implementados:

#### 📁 `domain/port/in/` - Portas de Entrada
- **`BankingUseCase.java`**: Interface que define os casos de uso do sistema
  - Por que aqui: Define o contrato do que o sistema deve fazer
  - Localização: `src/main/java/com/bank/.../domain/port/in/`

#### 📁 `domain/port/out/` - Portas de Saída
- **`AccountPersistencePort.java`**: Interface para persistência de dados
- **`EventPublishingPort.java`**: Interface para publicação de eventos
  - Por que aqui: Define contratos para recursos externos necessários
  - Localização: `src/main/java/com/bank/.../domain/port/out/`

#### 📁 `domain/service/` - Serviços de Domínio
- **`BankingDomainService.java`**: Implementa a lógica de negócio pura
  - Por que aqui: Contém todas as regras de negócio isoladas
  - Localização: `src/main/java/com/bank/.../domain/service/`

#### 📁 `adapter/in/` - Adaptadores de Entrada
- **`web/BankingController.java`**: Adapter REST para processamento síncrono
- **`messaging/AsyncBankingWorker.java`**: Adapter Kafka para processamento assíncrono
  - Por que aqui: Convertem requisições externas para chamadas do domínio
  - Localização: `src/main/java/com/bank/.../adapter/in/`

#### 📁 `adapter/out/` - Adaptadores de Saída
- **`persistence/AccountPersistenceAdapter.java`**: Implementa persistência PostgreSQL
- **`messaging/EventPublishingAdapter.java`**: Implementa publicação no Kafka
- **`messaging/AsyncBankingAdapter.java`**: Gerencia processamento assíncrono
  - Por que aqui: Implementam as interfaces de saída usando tecnologias específicas
  - Localização: `src/main/java/com/bank/.../adapter/out/`

---

## 🚦 Gateway Inteligente

### O que é e Por que?

O **API Gateway** foi implementado para:

1. **Ponto Único de Entrada**: Centraliza todas as requisições
2. **Roteamento Inteligente**: Decide entre processamento sync/async baseado na carga
3. **Monitoramento**: Controla CPU e conexões ativas
4. **Resiliência**: Evita sobrecarga do sistema

### Como Funciona:

```java
// Localização: src/main/java/com/bank/.../gateway/ApiGateway.java

@PostMapping("/accounts")
public ResponseEntity<?> createAccount(@RequestBody AccountCreationRequest request) {
    if (loadMonitor.shouldUseAsyncProcessing()) {
        // Alta carga: processa via Kafka
        return asyncAdapter.createAccountAsync(request);
    } else {
        // Carga normal: processa síncronamente
        return syncController.createAccount(request);
    }
}
```

### Monitoramento de Carga:

```java
// Localização: src/main/java/com/bank/.../metrics/SystemLoadMonitor.java

public boolean shouldUseAsyncProcessing() {
    double currentCpu = getCurrentCpuUsage();
    int currentConnections = getActiveConnections();
    
    return currentCpu > cpuThreshold || currentConnections > connectionThreshold;
}
```

**Configuração dos Thresholds:**
```properties
# application.properties
app.load.cpu-threshold=70.0
app.load.connection-threshold=100
```

---

## 🎨 Padrões de Design Implementados

### 1. **Hexagonal Architecture** (Ports and Adapters)
- **Onde**: Estrutura geral do projeto
- **Por que**: Isolamento do domínio e flexibilidade
- **Como**: Separação em domain, adapters e ports

### 2. **Gateway Pattern**
- **Onde**: `gateway/ApiGateway.java`
- **Por que**: Ponto único de entrada e roteamento inteligente
- **Como**: Gateway que roteia baseado na carga do sistema

### 3. **Adapter Pattern**
- **Onde**: `adapter/in/` e `adapter/out/`
- **Por que**: Integração com sistemas externos sem acoplar ao domínio
- **Como**: Interfaces no domínio, implementações nos adapters

### 4. **Event-Driven Architecture**
- **Onde**: Publicação de eventos via Kafka
- **Por que**: Desacoplamento e processamento assíncrono
- **Como**: `EventPublishingPort` e `EventPublishingAdapter`

### 5. **CQRS (Command Query Responsibility Segregation)**
- **Onde**: `cqrs/command/` e `cqrs/query/`
- **Por que**: Separação de responsabilidades entre comandos e consultas
- **Como**: Handlers específicos para cada tipo de operação

### 6. **Circuit Breaker Pattern**
- **Onde**: Configuração Resilience4j
- **Por que**: Previne cascata de falhas
- **Como**: Configurado via `application.properties`

### 7. **Saga Pattern**
- **Onde**: `saga/TransferSaga.java`
- **Por que**: Coordenação de transações distribuídas
- **Como**: Orquestração de múltiplas operações

### 8. **Observer Pattern**
- **Onde**: Sistema de eventos Kafka
- **Por que**: Notificação de mudanças de estado
- **Como**: Publicação e consumo de eventos

---

## 🛠️ Tecnologias e Ferramentas

### Backend Framework
- **Spring Boot 3.1.5**: Framework principal
- **Spring Data JPA**: Persistência de dados
- **Spring Kafka**: Integração com Apache Kafka

### Database
- **PostgreSQL**: Banco de dados principal
- **Flyway**: Versionamento de schema

### Messaging
- **Apache Kafka**: Message broker para eventos
- **Zookeeper**: Coordenação do Kafka

### Observabilidade
- **Micrometer**: Métricas
- **Prometheus**: Coleta de métricas
- **Zipkin**: Tracing distribuído
- **Logback**: Logs estruturados

### Resiliência
- **Resilience4j**: Circuit breaker, retry, rate limiting
- **Spring Boot Actuator**: Health checks

### Documentação
- **OpenAPI/Swagger**: Documentação da API

---

## 📂 Estrutura de Pastas

```
src/main/java/com/bank/BankingSystemApplication/
├── 🚪 gateway/
│   └── ApiGateway.java                    # Gateway inteligente
├── 🏛️ domain/
│   ├── port/
│   │   ├── in/
│   │   │   └── BankingUseCase.java        # Casos de uso
│   │   └── out/
│   │       ├── AccountPersistencePort.java # Interface persistência
│   │       └── EventPublishingPort.java   # Interface eventos
│   └── service/
│       └── BankingDomainService.java      # Lógica de negócio
├── 🔌 adapter/
│   ├── in/
│   │   ├── web/
│   │   │   └── BankingController.java     # REST Controller
│   │   └── messaging/
│   │       └── AsyncBankingWorker.java    # Kafka Consumer
│   └── out/
│       ├── persistence/
│       │   └── AccountPersistenceAdapter.java # Impl. persistência
│       └── messaging/
│           ├── EventPublishingAdapter.java     # Impl. eventos
│           └── AsyncBankingAdapter.java        # Async processing
├── 📊 metrics/
│   ├── SystemLoadMonitor.java            # Monitor de carga
│   └── BankingMetricsService.java        # Métricas customizadas
├── 🔧 config/
│   ├── KafkaConfig.java                  # Configuração Kafka
│   └── ResilienceConfig.java             # Configuração resiliência
└── ... (outros componentes existentes)
```

---

## 🔄 Fluxos de Operação

### Fluxo Síncrono (Carga Normal)

```
1. Cliente → API Gateway
2. Gateway verifica carga (CPU < 70%, Conexões < 100)
3. Gateway → BankingController
4. Controller → BankingDomainService
5. DomainService → AccountPersistenceAdapter
6. DomainService → EventPublishingAdapter (eventos)
7. Resposta retornada imediatamente
```

### Fluxo Assíncrono (Carga Alta)

```
1. Cliente → API Gateway
2. Gateway detecta alta carga (CPU > 70% OR Conexões > 100)
3. Gateway → AsyncBankingAdapter
4. AsyncAdapter → Kafka (publica mensagem)
5. Gateway retorna HTTP 202 (Accepted)
6. AsyncBankingWorker consome mensagem do Kafka
7. Worker → BankingDomainService
8. DomainService processa normalmente
9. Resultado publicado em tópico de resposta
```

### Exemplo de Requisição:

```bash
# Para testar o gateway
curl -X POST http://localhost:8080/api/gateway/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "cpf": "12345678901",
    "email": "joao@email.com",
    "phone": "11999999999",
    "birthDate": "1990-01-01"
  }'

# Resposta em carga normal (sync)
{
  "id": 1,
  "name": "João Silva",
  "balance": 0.00,
  ...
}

# Resposta em carga alta (async)
"Request accepted for async processing. Check status endpoint."
```

---

## 📈 Observabilidade e Monitoramento

### Métricas Disponíveis

#### Health Checks
- **`/actuator/health`**: Status geral do sistema
- **`/actuator/metrics`**: Métricas detalhadas
- **`/actuator/prometheus`**: Métricas para Prometheus

#### Métricas Customizadas (BankingMetricsService)
```java
// Localização: src/main/java/com/bank/.../metrics/BankingMetricsService.java

- banking.accounts.created.total     # Total de contas criadas
- banking.transactions.credit.total  # Total de créditos
- banking.transactions.debit.total   # Total de débitos
- banking.transactions.time         # Tempo de execução
- banking.accounts.creation.time    # Tempo de criação de conta
```

#### Monitoramento de Carga
```java
// Endpoint para verificar status de carga
GET /api/gateway/load-status

// Resposta
"CPU: 45.67%, Active Connections: 23, Processing Mode: SYNC"
```

### Logs Estruturados
```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "BankingDomainService",
  "message": "Domain: Account created successfully",
  "correlationId": "abc-123-def",
  "operation": "createAccount",
  "accountId": "12345"
}
```

### Auditoria Completa
```java
// Localização: src/main/java/com/bank/.../audit/BankingAuditService.java

- Todas as operações são auditadas
- Logs de auditoria em arquivo separado
- Rastreabilidade completa de transações
```

---

## ⚙️ Configurações Principais

### application.properties - Seções Importantes

#### Roteamento Baseado em Carga
```properties
# Thresholds para ativação do modo assíncrono
app.load.cpu-threshold=70.0
app.load.connection-threshold=100
```

#### Tópicos Kafka
```properties
kafka.topics.account-create=banking.account.create
kafka.topics.transaction-credit=banking.transaction.credit
kafka.topics.transaction-debit=banking.transaction.debit
kafka.topics.notifications=banking.notifications
```

#### Processamento Assíncrono
```properties
async.processing.enabled=true
async.processing.queue-capacity=1000
async.processing.core-pool-size=10
async.processing.max-pool-size=50
```

#### Resiliência (Resilience4j)
```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.banking-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.banking-service.wait-duration-in-open-state=30s

# Retry
resilience4j.retry.instances.banking-service.max-attempts=3
resilience4j.retry.instances.banking-service.wait-duration=500ms

# Rate Limiter
resilience4j.ratelimiter.instances.banking-api.limit-for-period=100
```

---

## 🧪 Como Testar

### 1. Subir o Sistema
```bash
# Com Docker
docker-compose up -d

# Ou localmente
./mvnw spring-boot:run
```

### 2. Testar Endpoints do Gateway

#### Criar Conta
```bash
curl -X POST http://localhost:8080/api/gateway/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "cpf": "12345678901",
    "email": "joao@email.com",
    "phone": "11999999999",
    "birthDate": "1990-01-01"
  }'
```

#### Operação de Crédito
```bash
curl -X POST http://localhost:8080/api/gateway/transactions/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 100.00
  }'
```

#### Verificar Carga do Sistema
```bash
curl http://localhost:8080/api/gateway/load-status
```

### 3. Testar Processamento Assíncrono

Para forçar o processamento assíncrono, você pode:

1. **Simular alta carga de CPU** (executar operações intensivas)
2. **Criar muitas conexões simultâneas**
3. **Ajustar os thresholds** para valores baixos nos properties

### 4. Monitoramento

#### Swagger UI
- http://localhost:8080/swagger-ui.html

#### Kafka UI
- http://localhost:8082

#### Métricas
- http://localhost:8080/actuator/metrics
- http://localhost:8080/actuator/health

#### PgAdmin (Database)
- http://localhost:8081

---

## 🎯 Benefícios da Arquitetura Implementada

### 1. **Escalabilidade Automática**
- Sistema se adapta automaticamente à carga
- Processamento assíncrono quando necessário
- Balanceamento transparente

### 2. **Resiliência**
- Circuit breakers previnem cascata de falhas
- Retry automático em falhas temporárias
- Rate limiting protege contra sobrecarga

### 3. **Observabilidade**
- Métricas detalhadas de performance
- Logs estruturados com correlação
- Tracing distribuído
- Auditoria completa

### 4. **Manutenibilidade**
- Código limpo e bem organizado
- Separação clara de responsabilidades
- Fácil adição de novos adapters
- Testabilidade aprimorada

### 5. **Flexibilidade**
- Fácil troca de tecnologias (database, messaging)
- Adição de novos canais de entrada
- Configuração via properties
- Deploy independente de componentes

---

## 📚 Conclusão

Esta implementação demonstra como uma **Arquitetura Hexagonal** bem estruturada, combinada com um **Gateway Inteligente** e **padrões modernos de desenvolvimento**, pode criar um sistema bancário robusto, escalável e resiliente.

A arquitetura permite que o sistema:
- **Se adapte automaticamente** à demanda
- **Mantenha alta disponibilidade** mesmo sob carga
- **Seja facilmente mantido e evoluído**
- **Forneça observabilidade completa** das operações

O resultado é um sistema pronto para produção que segue as melhores práticas da indústria.