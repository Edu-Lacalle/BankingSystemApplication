# 🛠️ Banking System - Development Guide

## Overview

This guide documents architectural decisions, design patterns, development practices, and implementation details for the Banking System. It serves as a comprehensive reference for developers working on the project.

## 🏗️ Architectural Decisions

### Hexagonal Architecture (Ports & Adapters)

**Decision**: Implement hexagonal architecture with intelligent API Gateway

**Rationale**:
- **Domain Isolation**: Business logic independent of frameworks and external systems
- **Testability**: Facilitates unit and integration testing with mock implementations
- **Flexibility**: Allows swapping implementations without affecting core business logic
- **Maintainability**: Clear separation of responsibilities and concerns

**Structure**:
```
domain/
├── port/in/          # Use cases (interfaces)
├── port/out/         # Infrastructure ports (interfaces)
└── service/          # Business logic implementation

adapter/
├── in/
│   ├── web/          # REST Controllers
│   └── messaging/    # Kafka Consumers
└── out/
    ├── persistence/  # Database implementations
    └── messaging/    # Event publishing implementations

infrastructure/
├── config/           # Configuration classes
├── monitoring/       # Observability components
└── exception/        # Error handling
```

**Benefits Achieved**:
- Easy substitution of implementations for testing
- Framework-independent business logic
- Independent deployment of layers
- Controlled architectural evolution

### Database Technology Selection

**Decision**: PostgreSQL as primary database

**Rationale**:
1. **ACID Compliance**: Banking transactions require absolute consistency
2. **Performance**: Optimized for complex transactional operations
3. **JSON Support**: Flexibility for storing metadata and audit information
4. **Ecosystem**: Mature tooling and extensive documentation
5. **Concurrency Control**: Optimistic locking with `@Version` annotation

**Configuration**:
```properties
# Optimizations for banking transactions
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
spring.flyway.enabled=true
spring.jpa.properties.hibernate.jdbc.batch_size=25
```

**Alternatives Considered**:
- **MySQL**: Discarded due to weaker support for complex transactions
- **Oracle**: Discarded due to cost and complexity
- **MongoDB**: Discarded for being NoSQL (inconsistent for banking operations)

### Message Broker Selection

**Decision**: Apache Kafka as primary message broker

**Rationale**:
1. **Event Streaming**: Ideal for event-driven architecture
2. **Durability**: Persists messages for audit and replay capabilities
3. **Throughput**: High performance for large message volumes
4. **Ordering**: Guarantees message order within partitions
5. **Replay Capability**: Ability to reprocess events for recovery

**Topics Implementation**:
```yaml
Banking Topics:
- banking.account.create     # Account creation requests
- banking.account.created    # Successfully created accounts
- banking.account.failed     # Account creation failures
- banking.transaction.credit # Credit operations
- banking.transaction.debit  # Debit operations
- banking.notifications      # User notifications
```

**Configuration for Consistency**:
```properties
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.producer.properties.enable.idempotence=true
```

## 🎨 Design Patterns Implementation

### 1. API Gateway Pattern

**Implementation**: Intelligent gateway with load-based routing

```java
@RestController
@RequestMapping("/api/gateway")
public class ApiGateway {
    
    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(AccountCreationRequest request) {
        if (systemLoadMonitor.shouldUseAsyncProcessing()) {
            // High load: route to async processing
            return asyncBankingAdapter.createAccountAsync(request);
        } else {
            // Normal load: route to synchronous processing
            return bankingController.createAccount(request);
        }
    }
}
```

**Benefits**:
- **Load Balancing**: Distributes load between sync/async processing
- **Circuit Breaking**: Protection against cascading failures
- **Centralized Monitoring**: Single point for observability
- **Rate Limiting**: Centralized traffic control

### 2. CQRS (Command Query Responsibility Segregation)

**Implementation**: Separate command and query operations

```java
// Commands
@Component
public class AccountCommandHandler {
    public CompletableFuture<Account> handle(CreateAccountCommand command) {
        // Command processing logic
    }
}

// Queries
@Component
public class AccountQueryHandler {
    public Account handle(AccountQuery query) {
        // Query processing logic
    }
}
```

**Benefits**:
- **Independent Scalability**: Optimize reads and writes separately
- **Simplified Models**: Operation-specific models
- **Performance**: Optimized queries without impacting commands

### 3. Saga Pattern

**Implementation**: Orchestration saga for distributed transactions

```java
@Component
public class TransferSaga {
    
    @SagaStart
    public SagaResult executeTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        try {
            // Step 1: Debit from source account
            TransactionResult debitResult = debitAccount(fromAccountId, amount);
            
            // Step 2: Credit to target account
            TransactionResult creditResult = creditAccount(toAccountId, amount);
            
            return SagaResult.success();
        } catch (Exception e) {
            // Automatic compensation
            compensateTransfer(fromAccountId, toAccountId, amount);
            return SagaResult.failure(e);
        }
    }
}
```

**Benefits**:
- **Distributed Transactions**: No 2PC required
- **Automatic Compensation**: Rollback on failures
- **Transaction Visibility**: Trackable transaction state

### 4. Decorator Pattern for Cross-Cutting Concerns

**Implementation**: Layered decorators for different responsibilities

```java
// Resilience Decorator
@Component
public class ResilientAccountService implements AccountService {
    
    @CircuitBreaker(name = "banking-service")
    @Retry(name = "banking-service")
    @TimeLimiter(name = "banking-service")
    public Account createAccount(AccountCreationRequest request) {
        return delegate.createAccount(request);
    }
}

// Async Decorator
@Component
public class AsyncAccountService implements AccountService {
    
    public Account createAccount(AccountCreationRequest request) {
        Account account = delegate.createAccount(request);
        eventPublisher.publishAccountCreated(account);
        notificationService.sendWelcomeEmail(account);
        return account;
    }
}
```

**Benefits**:
- **Single Responsibility**: Each decorator has one concern
- **Composition**: Easy combination of behaviors
- **Testability**: Isolated testing of each concern

## 🔧 Technology Stack Decisions

### Spring Boot 3.1.5

**Decision**: Spring Boot as primary framework

**Rationale**:
1. **Maturity**: Established enterprise framework
2. **Ecosystem**: Native integration with Kafka, PostgreSQL, etc.
3. **Jakarta EE**: Migration to modern standards
4. **Observability**: Actuator for comprehensive monitoring
5. **Cloud Native**: Container and Kubernetes ready

**Key Dependencies**:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

### Java 17 LTS

**Decision**: Java 17 as runtime platform

**Rationale**:
- **LTS Support**: Long-term support until 2029
- **Performance**: Significant GC improvements
- **Language Features**: Records, pattern matching, sealed classes
- **Security**: Latest security improvements

**Usage Examples**:
```java
// Records for DTOs
public record AccountCreationRequest(
    @NotBlank String name,
    @Pattern(regexp = "\\d{11}") String cpf,
    @NotNull LocalDate birthDate,
    @Email String email
) {}

// Pattern matching (preview)
public String processTransactionResult(TransactionResult result) {
    return switch (result.status()) {
        case SUCCESS -> "Transaction completed successfully";
        case FAILURE -> "Transaction failed: " + result.errorMessage();
        case PENDING -> "Transaction is being processed";
    };
}

// Sealed classes for type safety
public sealed interface TransactionEvent 
    permits CreditEvent, DebitEvent, TransferEvent {
    
    String getAccountId();
    BigDecimal getAmount();
}
```

### Resilience4j

**Decision**: Resilience4j for resilience patterns

**Rationale**:
- **Spring Boot 3 Compatibility**: Native integration
- **Lightweight**: Lower overhead than alternatives
- **Composability**: Easy combination of multiple patterns
- **Comprehensive**: Circuit breaker, retry, rate limiting, bulkhead

**Configuration**:
```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.banking-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.banking-service.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.banking-service.sliding-window-size=10

# Retry
resilience4j.retry.instances.banking-service.max-attempts=3
resilience4j.retry.instances.banking-service.wait-duration=500ms
resilience4j.retry.instances.banking-service.exponential-backoff-multiplier=2

# Rate Limiter
resilience4j.ratelimiter.instances.banking-api.limit-for-period=100
resilience4j.ratelimiter.instances.banking-api.limit-refresh-period=1m
resilience4j.ratelimiter.instances.banking-api.timeout-duration=5s
```

## 📊 Observability Strategy

### Three Pillars Implementation

**Decision**: Comprehensive observability with metrics, logs, and traces

**Implementation Stack**:
1. **Metrics**: Micrometer + Prometheus + Datadog
2. **Logs**: Logback + Structured JSON + Correlation IDs
3. **Traces**: Zipkin + Spring Cloud Sleuth + Datadog APM

### Custom Business Metrics

**Implementation**:
```java
@Component
public class BankingMetricsService {
    
    private final Counter accountsCreatedCounter;
    private final Counter transactionCounter;
    private final Timer responseTimeTimer;
    private final Gauge balanceGauge;
    
    public void recordAccountCreated(Account account) {
        accountsCreatedCounter.increment(
            Tags.of(
                "account.type", account.getType(),
                "region", account.getRegion()
            )
        );
    }
    
    public void recordTransaction(TransactionType type, BigDecimal amount) {
        transactionCounter.increment(
            Tags.of(
                "type", type.toString(),
                "amount.range", getAmountRange(amount)
            )
        );
    }
}
```

**Metrics Categories**:
- **Technical Metrics**: Response time, throughput, error rates
- **Business Metrics**: Accounts created, transaction volume, balance totals
- **Infrastructure Metrics**: JVM, database, Kafka metrics
- **Resilience Metrics**: Circuit breaker state, retry attempts

### Structured Logging

**Configuration**:
```xml
<!-- logback-spring.xml -->
<springProfile name="production">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <version/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
                <pattern>
                    <pattern>
                        {
                            "service": "banking-system",
                            "environment": "${ENVIRONMENT:-development}",
                            "trace_id": "%X{traceId:-}",
                            "span_id": "%X{spanId:-}",
                            "request_id": "%X{requestId:-}"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
    </appender>
</springProfile>
```

## 🛡️ Resilience Strategy

### Defense in Depth

**Implementation**: Multiple layers of resilience patterns

**Layers**:
1. **API Gateway**: Rate limiting, circuit breaking, load balancing
2. **Service Layer**: Retry policies, timeouts, bulkhead isolation
3. **Database Layer**: Connection pooling, query timeouts, read replicas
4. **Messaging Layer**: Dead letter queues, retry policies, idempotency

### Circuit Breaker Configuration

**Strategy**: Adaptive circuit breaker with configurable thresholds

```java
@Component
public class ResilientBankingService {
    
    @CircuitBreaker(name = "banking-service", fallbackMethod = "fallbackCreateAccount")
    @Retry(name = "banking-service")
    @TimeLimiter(name = "banking-service")
    public CompletableFuture<Account> createAccountResilient(AccountCreationRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            domainService.createAccount(request)
        );
    }
    
    public CompletableFuture<Account> fallbackCreateAccount(
            AccountCreationRequest request, Exception exception) {
        // Graceful degradation
        logger.warn("Creating account in offline mode due to: {}", exception.getMessage());
        return CompletableFuture.completedFuture(
            createAccountOffline(request)
        );
    }
}
```

**States**:
- **CLOSED**: Normal operation (failure rate < 50%)
- **OPEN**: Fast fail mode (failure rate ≥ 50%) for 30 seconds
- **HALF_OPEN**: Gradual recovery testing

## 🔄 Development Practices

### Code Organization

**Package Structure**:
```
src/main/java/com/bank/BankingSystemApplication/
├── domain/                    # Core business logic
│   ├── model/                # Entities and value objects
│   ├── port/                 # Interface definitions
│   └── service/              # Domain services
├── application/              # Application services
│   ├── service/              # Use case implementations
│   ├── cqrs/                 # Command/Query separation
│   └── saga/                 # Transaction orchestration
├── adapter/                  # External integrations
│   ├── in/web/              # REST controllers
│   ├── in/messaging/        # Message consumers
│   ├── out/persistence/     # Database adapters
│   └── out/messaging/       # Message publishers
├── infrastructure/          # Technical concerns
│   ├── config/              # Configuration
│   ├── monitoring/          # Observability
│   └── exception/           # Error handling
└── dto/                     # Data transfer objects
    ├── request/             # Request DTOs
    └── response/            # Response DTOs
```

### Error Handling Strategy

**Standardized Error Response**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request),
            requestId
        );
        
        logger.warn("Account not found - RequestId: {} - Message: {}", 
            requestId, ex.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
}
```

**Error Response Format**:
```json
{
  "error": "ACCOUNT_NOT_FOUND",
  "message": "Account not found with ID: 123",
  "timestamp": "2024-01-15T10:30:45.123",
  "path": "/api/gateway/accounts/123",
  "requestId": "req_abc123def456",
  "details": {
    "accountId": "123"
  }
}
```

### Testing Strategy

**Testing Pyramid**:
1. **Unit Tests**: Domain logic, individual components
2. **Integration Tests**: Database, message broker integration
3. **Contract Tests**: API contract validation
4. **End-to-End Tests**: Complete user scenarios

**Test Configuration**:
```java
@SpringBootTest
@Testcontainers
class BankingSystemIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("banking_test")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    
    @Test
    void shouldCreateAccountSuccessfully() {
        // Test implementation
    }
}
```

## 🚀 Performance Optimization

### Load-Based Routing

**Implementation**: Dynamic routing based on system metrics

```java
@Component
public class SystemLoadMonitor {
    
    private final MeterRegistry meterRegistry;
    
    public boolean shouldUseAsyncProcessing() {
        double currentCpu = getCurrentCpuUsage();
        int activeConnections = getActiveConnections();
        double avgResponseTime = getAverageResponseTime();
        
        return currentCpu > cpuThreshold || 
               activeConnections > connectionThreshold ||
               avgResponseTime > responseTimeThreshold;
    }
    
    private double getCurrentCpuUsage() {
        return meterRegistry.get("system.cpu.usage").gauge().value() * 100;
    }
    
    private int getActiveConnections() {
        return (int) meterRegistry.get("tomcat.sessions.active.current").gauge().value();
    }
}
```

**Threshold Configuration**:
```properties
# Load balancing thresholds
app.load.cpu-threshold=70.0
app.load.connection-threshold=100
app.load.response-time-threshold=500.0
```

### Caching Strategy

**Multi-Level Caching**:
```java
@Service
public class AccountQueryService {
    
    @Cacheable(value = "accounts", key = "#accountId")
    public Account findById(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
    
    @CacheEvict(value = "accounts", key = "#account.id")
    public void evictAccount(Account account) {
        // Cache invalidation on updates
    }
}
```

**Cache Configuration**:
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m
```

## 🔐 Security Implementation

### Input Validation

**Bean Validation**:
```java
public record AccountCreationRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "\\d{11}", message = "CPF must have exactly 11 digits")
    String cpf,
    
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    LocalDate birthDate,
    
    @Email(message = "Invalid email format")
    String email
) {}
```

### Audit Trail

**Implementation**:
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Account {
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String lastModifiedBy;
    
    @Version
    private Long version;
}
```

## 🎯 Future Roadmap

### Planned Enhancements

1. **Kubernetes Migration**: Container orchestration
2. **Complete Event Sourcing**: Full event-driven architecture
3. **Read Model Optimization**: CQRS read model implementation
4. **Machine Learning**: Fraud detection with ML algorithms
5. **Multi-tenancy**: Support for multiple banking institutions

### Technical Debt Considerations

1. **Complexity vs Flexibility**: Accepted architectural complexity for flexibility
2. **Performance vs Consistency**: Prioritized consistency over extreme performance
3. **Storage vs Audit**: Storage overhead for complete audit trail
4. **Learning Curve**: Team training on hexagonal architecture patterns

## 📚 Development Guidelines

### Code Standards

1. **Clean Code**: Follow Robert Martin's clean code principles
2. **SOLID Principles**: Single responsibility, open/closed, Liskov substitution, interface segregation, dependency inversion
3. **DRY**: Don't repeat yourself - extract common functionality
4. **YAGNI**: You aren't gonna need it - avoid over-engineering

### Git Workflow

1. **Feature Branches**: One feature per branch
2. **Conventional Commits**: Structured commit messages
3. **Pull Requests**: Code review before merging
4. **CI/CD**: Automated testing and deployment

### Documentation Standards

1. **Code Documentation**: JavaDoc for public APIs
2. **Architecture Decision Records**: Document significant decisions
3. **API Documentation**: OpenAPI/Swagger specifications
4. **Runbooks**: Operational procedures documentation

This development guide provides the foundation for maintaining and evolving the Banking System while preserving its architectural integrity and operational excellence.