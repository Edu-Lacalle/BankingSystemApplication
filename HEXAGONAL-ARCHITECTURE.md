# Arquitetura Hexagonal - Sistema Bancário

## Visão Geral da Arquitetura

Este projeto implementa uma arquitetura hexagonal (Ports and Adapters) com roteamento inteligente baseado na carga do sistema, alternando entre processamento síncrono e assíncrono.

```
┌─────────────────┐    ┌──────────────────┐
│   Cliente Web   │───▶│    API Gateway   │
└─────────────────┘    └──────────────────┘
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
            ┌──────────────┐ ┌─────────┐ ┌──────────────┐
            │ REST Service │ │  Kafka  │ │ Async Worker │
            └──────────────┘ └─────────┘ └──────────────┘
                    │           │           │
                    └───────────┼───────────┘
                                ▼
                    ┌──────────────────────┐
                    │   Core Banking       │
                    │   Service            │
                    └──────────────────────┘
                                │
                                ▼
                    ┌──────────────────────┐
                    │     Database         │
                    │   (Consistência)     │
                    └──────────────────────┘
```

## Componentes da Arquitetura

### 1. API Gateway (`ApiGateway.java`)
- **Responsabilidade**: Ponto de entrada único para todas as requisições
- **Funcionalidades**:
  - Monitoramento de carga do sistema (CPU e conexões ativas)
  - Roteamento inteligente (síncrono vs assíncrono)
  - Endpoints de status e saúde

### 2. Domain Layer (Hexágono Central)

#### Ports (Interfaces)
- **`BankingUseCase`**: Define os casos de uso do domínio
- **`AccountPersistencePort`**: Interface para persistência de dados
- **`EventPublishingPort`**: Interface para publicação de eventos

#### Domain Service
- **`BankingDomainService`**: Implementa a lógica de negócio pura
- Isolado de detalhes técnicos (framework, database, messaging)

### 3. Adapters (Camadas Externas)

#### Input Adapters (Drivers)
- **`BankingController`**: Adapter REST para processamento síncrono
- **`AsyncBankingWorker`**: Adapter Kafka para processamento assíncrono

#### Output Adapters (Driven)
- **`AccountPersistenceAdapter`**: Adapter para PostgreSQL
- **`EventPublishingAdapter`**: Adapter para Kafka
- **`AsyncBankingAdapter`**: Adapter para processamento assíncrono

### 4. Sistema de Monitoramento
- **`SystemLoadMonitor`**: Monitora CPU e conexões ativas
- **`BankingMetricsService`**: Coleta métricas de performance
- **`BankingAuditService`**: Auditoria de operações

## Fluxo de Processamento

### Processamento Síncrono (Carga Normal)
1. Cliente envia requisição para API Gateway
2. Gateway verifica carga do sistema
3. Se carga baixa: roteia para `BankingController`
4. Controller chama `BankingDomainService`
5. Domain Service executa lógica de negócio
6. Resposta retornada imediatamente

### Processamento Assíncrono (Carga Alta)
1. Cliente envia requisição para API Gateway
2. Gateway detecta alta carga
3. Roteia para `AsyncBankingAdapter`
4. Adapter publica mensagem no Kafka
5. Retorna confirmação de aceite (HTTP 202)
6. `AsyncBankingWorker` consome mensagem
7. Worker executa processamento via `BankingDomainService`
8. Resultado publicado em tópico de resposta

## Configuração de Carga

### Parâmetros de Threshold
```properties
# Limites para roteamento assíncrono
app.load.cpu-threshold=70.0
app.load.connection-threshold=100
```

### Tópicos Kafka
```properties
kafka.topics.account-create=banking.account.create
kafka.topics.transaction-credit=banking.transaction.credit
kafka.topics.transaction-debit=banking.transaction.debit
kafka.topics.notifications=banking.notifications
```

## Benefícios da Arquitetura

### 1. Desacoplamento
- Domain isolado de frameworks e tecnologias
- Fácil troca de adapters sem afetar lógica de negócio
- Testabilidade aprimorada

### 2. Escalabilidade
- Roteamento automático baseado em carga
- Processamento assíncrono para alta demanda
- Balanceamento de carga transparente

### 3. Observabilidade
- Métricas de performance
- Auditoria completa
- Tracing distribuído
- Logs estruturados

### 4. Resiliência
- Circuit breakers
- Retry patterns
- Rate limiting
- Graceful degradation

## Endpoints Principais

### API Gateway
- `POST /api/gateway/accounts` - Criação de conta
- `POST /api/gateway/transactions/credit` - Operação de crédito
- `POST /api/gateway/transactions/debit` - Operação de débito
- `GET /api/gateway/load-status` - Status de carga do sistema

### REST Síncrono
- `POST /api/sync/accounts` - Criação síncrona
- `POST /api/sync/transactions/credit` - Crédito síncrono
- `POST /api/sync/transactions/debit` - Débito síncrono

## Monitoramento e Métricas

### Health Checks
- `/actuator/health` - Status geral do sistema
- `/actuator/metrics` - Métricas detalhadas
- `/actuator/prometheus` - Métricas para Prometheus

### Métricas Customizadas
- Tempo de execução de transações
- Contadores de operações
- Taxa de sucesso/falha
- Carga do sistema em tempo real

## Padrões Implementados

1. **Hexagonal Architecture**: Separação clara entre domínio e infraestrutura
2. **CQRS**: Separação de comandos e consultas
3. **Event Sourcing**: Publicação de eventos de domínio
4. **Circuit Breaker**: Resiliência em operações externas
5. **Saga Pattern**: Coordenação de transações distribuídas
6. **Gateway Pattern**: Ponto único de entrada
7. **Adapter Pattern**: Integração com sistemas externos

Esta arquitetura garante alta disponibilidade, escalabilidade e manutenibilidade do sistema bancário.