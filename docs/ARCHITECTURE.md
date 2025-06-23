# 🏗️ Banking System - Architecture Documentation

## Overview

This banking system implements a comprehensive **Hexagonal Architecture (Ports & Adapters)** with intelligent load-based routing, event-driven processing, and complete observability.

## Architectural Principles

### 🔵 Domain-Driven Design
- **Pure Business Logic**: Domain contains only business rules and logic
- **Framework Independence**: Domain layer has no external dependencies
- **Testability**: Core logic can be tested in isolation

### 🔄 Hexagonal Architecture Structure

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

## Core Components

### 🎯 Domain Layer
```
domain/
├── model/           # Entities and Value Objects
├── port/
│   ├── in/         # Input Ports (Use Cases)
│   └── out/        # Output Ports (Repository interfaces)
└── service/        # Domain Services (Business Logic)
```

**Key Classes:**
- `BankingUseCase.java` - Defines business operations
- `BankingDomainService.java` - Implements core business logic
- `AccountPersistencePort.java` - Data persistence contract
- `EventPublishingPort.java` - Event publishing contract

### 🔌 Application Layer
```
application/
├── service/        # Application Services
├── cqrs/          # Command Query Responsibility Segregation
│   ├── command/   # Command handlers
│   ├── query/     # Query handlers
│   └── handler/   # Implementation
└── saga/          # Transaction orchestration
```

**Responsibilities:**
- Orchestrate use cases
- Handle CQRS operations
- Manage transaction sagas
- Coordinate async processing

### 🔗 Adapter Layer
```
adapter/
├── in/            # Input Adapters (Controllers)
│   ├── web/       # REST Controllers
│   └── messaging/ # Kafka Consumers
└── out/           # Output Adapters (Implementations)
    ├── persistence/ # Database implementations
    └── messaging/   # Event publishing implementations
```

## 🚦 Intelligent API Gateway

### Load-Based Routing
The API Gateway implements intelligent routing based on system load:

```java
@PostMapping("/accounts")
public ResponseEntity<?> createAccount(@RequestBody AccountCreationRequest request) {
    if (systemLoadMonitor.shouldUseAsyncProcessing()) {
        // High load: Route to async processing
        return asyncBankingAdapter.createAccountAsync(request);
    } else {
        // Normal load: Route to synchronous processing
        return bankingController.createAccount(request);
    }
}
```

### Load Monitoring
```java
public boolean shouldUseAsyncProcessing() {
    double currentCpu = getCurrentCpuUsage();
    int activeConnections = getActiveConnections();
    
    return currentCpu > cpuThreshold || activeConnections > connectionThreshold;
}
```

**Thresholds Configuration:**
```properties
# When to switch to async processing
app.load.cpu-threshold=70.0
app.load.connection-threshold=100
```

## 🔄 Processing Flows

### Synchronous Flow (Normal Load)
```
1. Client → API Gateway
2. Gateway checks load (CPU < 70%, Connections < 100)
3. Gateway → BankingController
4. Controller → BankingDomainService
5. DomainService → AccountPersistenceAdapter
6. DomainService → EventPublishingAdapter (events)
7. Immediate response returned
```

### Asynchronous Flow (High Load)
```
1. Client → API Gateway
2. Gateway detects high load (CPU > 70% OR Connections > 100)
3. Gateway → AsyncBankingAdapter
4. AsyncAdapter → Kafka (publishes message)
5. Gateway returns HTTP 202 (Accepted)
6. AsyncBankingWorker consumes Kafka message
7. Worker → BankingDomainService
8. DomainService processes normally
9. Result published to response topic
```

## 🎨 Design Patterns Implemented

### 1. Hexagonal Architecture (Ports & Adapters)
- **Location**: Entire project structure
- **Purpose**: Domain isolation and flexibility
- **Implementation**: Clear separation between domain, adapters, and ports

### 2. Gateway Pattern
- **Location**: `ApiGateway.java`
- **Purpose**: Single entry point and intelligent routing
- **Implementation**: Load-aware routing between sync/async processing

### 3. Event-Driven Architecture
- **Location**: Kafka integration throughout
- **Purpose**: Decoupling and async processing
- **Implementation**: Event publishing and consuming via Kafka

### 4. CQRS (Command Query Responsibility Segregation)
- **Location**: `application/cqrs/`
- **Purpose**: Separation of read and write operations
- **Implementation**: Separate command and query handlers

### 5. Circuit Breaker Pattern
- **Location**: Resilience4j configuration
- **Purpose**: Prevent cascading failures
- **Implementation**: Automatic failure detection and recovery

### 6. Saga Pattern
- **Location**: `application/saga/`
- **Purpose**: Distributed transaction coordination
- **Implementation**: Orchestration of multiple operations

## 📊 Technology Stack

### Core Framework
- **Spring Boot 3.1.5** - Main application framework
- **Java 17** - Programming language
- **Maven** - Build and dependency management

### Data Layer
- **PostgreSQL 15** - Primary database
- **Spring Data JPA** - Data access abstraction
- **Flyway** - Database migration tool

### Messaging
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **Zookeeper** - Kafka coordination

### Observability
- **Micrometer** - Metrics collection
- **Datadog APM** - Application performance monitoring
- **Zipkin** - Distributed tracing
- **Logback** - Structured logging

### Resilience
- **Resilience4j** - Circuit breaker, retry, rate limiting
- **Spring Boot Actuator** - Health checks and metrics

## 🏗️ Directory Structure

```
src/main/java/com/bank/BankingSystemApplication/
├── 🚪 adapter/
│   ├── in/
│   │   ├── web/
│   │   │   ├── ApiGateway.java           # Intelligent gateway
│   │   │   ├── BankingController.java    # Sync REST controller
│   │   │   └── AsyncAccountController.java # Async REST controller
│   │   └── messaging/
│   │       └── AsyncBankingWorker.java   # Kafka consumer
│   └── out/
│       ├── persistence/
│       │   └── AccountPersistenceAdapter.java # JPA implementation
│       └── messaging/
│           ├── EventPublishingAdapter.java    # Kafka producer
│           └── AsyncBankingAdapter.java       # Async processing
├── 🏛️ domain/
│   ├── model/
│   │   ├── Account.java                  # Main entity
│   │   ├── TransactionRequest.java       # Value objects
│   │   └── TransactionEvent.java         # Domain events
│   ├── port/
│   │   ├── in/
│   │   │   └── BankingUseCase.java       # Use case interface
│   │   └── out/
│   │       ├── AccountPersistencePort.java # Persistence contract
│   │       └── EventPublishingPort.java   # Event contract
│   └── service/
│       └── BankingDomainService.java     # Business logic
├── 🔧 application/
│   ├── service/
│   │   ├── AccountService.java           # Application service
│   │   └── AsyncAccountService.java      # Async decorator
│   ├── cqrs/
│   │   ├── command/                      # Command objects
│   │   ├── query/                        # Query objects
│   │   └── handler/                      # CQRS handlers
│   └── saga/
│       └── TransferSaga.java             # Transaction orchestration
└── 🔴 infrastructure/
    ├── config/
    │   ├── KafkaConfig.java              # Kafka configuration
    │   ├── DatadogConfiguration.java     # Monitoring setup
    │   └── ResilienceConfig.java         # Circuit breaker config
    ├── monitoring/
    │   ├── SystemLoadMonitor.java        # Load monitoring
    │   ├── BankingMetricsService.java    # Business metrics
    │   └── PerformanceMetricsService.java # Performance tracking
    └── exception/
        └── GlobalExceptionHandler.java   # Error handling
```

## 📈 Observability & Monitoring

### Health Checks
- `/actuator/health` - System health status
- `/actuator/metrics` - Detailed metrics
- `/actuator/prometheus` - Prometheus-compatible metrics

### Custom Metrics
```java
// Banking-specific metrics
banking.accounts.created.total      // Total accounts created
banking.transactions.credit.total   // Total credit transactions
banking.transactions.debit.total    // Total debit transactions
banking.transactions.time          // Transaction execution time
banking.system.load.cpu            // Current CPU usage
banking.system.connections.active  // Active connections
```

### Datadog Integration
- **APM Agent**: Automatic performance monitoring
- **Custom Metrics**: Business-specific measurements
- **Distributed Tracing**: End-to-end request tracking
- **Log Correlation**: Unified logging and tracing

## 🔒 Security & Resilience

### Input Validation
- CPF uniqueness and format validation
- Email format validation
- Positive amount validation
- Required field validation

### Concurrency Control
- Optimistic locking via `@Version`
- Transaction isolation
- Retry mechanisms for transient failures

### Circuit Breaker Configuration
```properties
# Failure threshold: 50% failure rate triggers circuit breaker
resilience4j.circuitbreaker.instances.banking-service.failure-rate-threshold=50
# Wait time in open state: 30 seconds
resilience4j.circuitbreaker.instances.banking-service.wait-duration-in-open-state=30s

# Retry configuration
resilience4j.retry.instances.banking-service.max-attempts=3
resilience4j.retry.instances.banking-service.wait-duration=500ms
```

## 🔧 Configuration

### Database Configuration
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/banking_system
spring.datasource.username=banking_user
spring.datasource.password=banking_password
spring.flyway.enabled=true
```

### Kafka Configuration
```properties
spring.kafka.bootstrap-servers=localhost:9092
kafka.topics.account-create=banking.account.create
kafka.topics.transaction-credit=banking.transaction.credit
kafka.topics.transaction-debit=banking.transaction.debit
kafka.topics.notifications=banking.notifications
```

### Async Processing
```properties
async.processing.enabled=true
async.processing.queue-capacity=1000
async.processing.core-pool-size=10
async.processing.max-pool-size=50
```

## 🎯 Benefits of This Architecture

### 1. **Automatic Scalability**
- System adapts to load automatically
- Async processing when needed
- Transparent load balancing

### 2. **High Resilience**
- Circuit breakers prevent cascade failures
- Automatic retry for transient failures
- Rate limiting protects against overload

### 3. **Complete Observability**
- Detailed performance metrics
- Structured logging with correlation
- Distributed tracing
- Complete audit trail

### 4. **Maintainability**
- Clean, organized code structure
- Clear separation of concerns
- Easy addition of new adapters
- Enhanced testability

### 5. **Flexibility**
- Easy technology swapping (database, messaging)
- Multiple input channels supported
- Configuration-driven behavior
- Independent component deployment

This architecture creates a production-ready banking system that follows industry best practices and can handle real-world scale and complexity.