# 📊 Banking System - Monitoring & Observability

## Overview

The Banking System implements comprehensive monitoring and observability with multiple layers:
- **Performance Monitoring**: Real-time metrics, throughput, and bottleneck detection
- **Datadog APM**: Application performance monitoring with distributed tracing
- **Business Metrics**: Banking-specific operational metrics
- **Health Checks**: System health and component status monitoring

## 🎯 Monitoring Components

### 1. Performance Monitoring System

#### PerformanceMetricsService
**Location**: `src/main/java/.../infrastructure/monitoring/PerformanceMetricsService.java`

**Capabilities**:
- **Throughput Measurement**: TPS (Transactions Per Second) and QPS (Queries Per Second)
- **Response Time Analytics**: Latency metrics with percentiles (P95, P99)
- **Bottleneck Detection**: Automatic identification of slow components
- **Alert Thresholds**: Configurable performance thresholds

**Key Metrics Collected**:
```java
// Throughput Metrics
banking.requests.total                      // Total requests received
banking.responses.total                     // Total responses sent
banking.throughput.requests.per.second      // Current TPS
banking.throughput.transactions.per.second  // Transaction TPS

// Response Time Metrics
banking.request.duration                    // Full request duration
banking.response.time                       // Response time by endpoint
banking.operation.time                      // Time per operation

// Bottleneck Metrics
banking.bottleneck.database.time           // Database operation time
banking.bottleneck.kafka.time             // Kafka operation time
banking.bottleneck.business.time           // Business logic time
banking.bottleneck.detected                // Bottleneck detection counter
```

#### PerformanceInterceptor
**Location**: `src/main/java/.../infrastructure/monitoring/PerformanceInterceptor.java`

**Functions**:
- Intercepts all HTTP requests
- Measures end-to-end processing time
- Records response sizes
- Automatically detects slow operations
- Structured logging with MDC context

**Sample Log Output**:
```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "message": "Request completed: POST /api/gateway/accounts - 245ms - 200 - 1024bytes",
  "endpoint": "/api/gateway/accounts",
  "method": "POST",
  "duration": "245",
  "status": "success",
  "responseSize": "1024",
  "requestId": "req_abc123def456"
}
```

#### PerformanceAspect (AOP)
**Location**: `src/main/java/.../infrastructure/monitoring/PerformanceAspect.java`

**Component Monitoring**:
- **Database Operations**: `@Around("execution(* ...repository.*.*(..))")`
- **Kafka Operations**: `@Around("execution(* ...kafka.*.*(..))")`
- **Business Logic**: `@Around("execution(* ...domain.service.*.*(..))")`
- **Messaging**: `@Around("execution(* ...messaging.*.*(..))")`

**Automatic Bottleneck Detection**:
```java
if (performanceMetricsService.isSlowOperation(duration)) {
    performanceMetricsService.recordBottleneck("database", operation, duration);
    logger.warn("Slow database operation: {} took {}ms", operation, duration.toMillis());
}
```

### 2. Datadog APM Integration

#### DatadogConfiguration
**Location**: `src/main/java/.../infrastructure/config/DatadogConfiguration.java`

**Features**:
- Metrics export to Datadog
- Custom tags for service identification
- Environment-specific configuration
- Automatic service discovery

**Configuration**:
```properties
# Datadog Settings
management.metrics.export.datadog.enabled=true
management.metrics.export.datadog.api-key=c576fa9380cc70a22dee72e7df176697
management.metrics.export.datadog.site=us5.datadoghq.com
datadog.environment=production
datadog.service=banking-system
datadog.version=1.0.0
```

#### DatadogTracingService
**Location**: `src/main/java/.../infrastructure/monitoring/DatadogTracingService.java`

**Distributed Tracing**:
- Custom business operation tracing
- Error correlation and tracking
- Performance profiling
- Cross-service trace correlation

**Usage Example**:
```java
@Autowired
private DatadogTracingService tracingService;

public void processTransfer(String fromAccount, String toAccount, BigDecimal amount) {
    Span span = tracingService.startTransfer(fromAccount, toAccount, amount);
    try {
        // Business logic execution
        span.tag("transfer.status", "success");
    } catch (Exception e) {
        tracingService.finishSpanWithError(span, e);
        throw e;
    } finally {
        tracingService.finishSpan(span);
    }
}
```

### 3. System Load Monitoring

#### SystemLoadMonitor
**Location**: `src/main/java/.../infrastructure/monitoring/SystemLoadMonitor.java`

**Monitoring Capabilities**:
- Real-time CPU usage tracking
- Active connection monitoring
- Load-based routing decisions
- Performance threshold management

**Load Assessment**:
```java
public boolean shouldUseAsyncProcessing() {
    double currentCpu = getCurrentCpuUsage();
    int activeConnections = getActiveConnections();
    
    return currentCpu > cpuThreshold || activeConnections > connectionThreshold;
}
```

**Configuration**:
```properties
# Load balancing thresholds
app.load.cpu-threshold=70.0
app.load.connection-threshold=100
```

## 📈 Monitoring Endpoints

### Performance Analytics

#### Complete Statistics
```bash
GET /api/performance/stats
```

**Response**:
```json
{
  "currentTPS": 45.2,
  "currentTransactionTPS": 38.7,
  "averageResponseTime": 234.5,
  "p95ResponseTime": 450.2,
  "p99ResponseTime": 890.1,
  "bottleneckInfo": {
    "databaseTime": 123,
    "kafkaTime": 56,
    "businessLogicTime": 89,
    "currentBottleneck": "database"
  },
  "slowestOperation": {
    "operation": "AccountRepository.findByIdForUpdate",
    "timeMs": 1250
  }
}
```

#### Throughput Metrics
```bash
GET /api/performance/throughput
```

**Response**:
```json
{
  "currentTPS": 45.2,
  "currentTransactionTPS": 38.7,
  "description": {
    "currentTPS": "Requests per second",
    "currentTransactionTPS": "Transactions per second"
  },
  "timestamp": 1705312200000
}
```

#### Response Time Analysis
```bash
GET /api/performance/response-time
```

**Response**:
```json
{
  "averageResponseTime": 234.5,
  "p95ResponseTime": 450.2,
  "p99ResponseTime": 890.1,
  "unit": "milliseconds",
  "description": {
    "averageResponseTime": "Average response time",
    "p95ResponseTime": "95% of requests served within",
    "p99ResponseTime": "99% of requests served within"
  }
}
```

#### Bottleneck Detection
```bash
GET /api/performance/bottlenecks
```

**Response**:
```json
{
  "componentPerformance": {
    "database": {
      "timeMs": 1250,
      "status": "slow"
    },
    "kafka": {
      "timeMs": 56,
      "status": "normal"
    },
    "businessLogic": {
      "timeMs": 89,
      "status": "normal"
    }
  },
  "currentBottleneck": "database",
  "slowestOperation": {
    "operation": "AccountRepository.findByIdForUpdate",
    "timeMs": 1250
  },
  "recommendations": {
    "database": "Consider optimizing queries, adding indexes, or increasing connection pool"
  }
}
```

#### Performance Dashboard
```bash
GET /api/performance/dashboard
```

**Response**:
```json
{
  "mainMetrics": {
    "tps": 45.2,
    "transactionTps": 38.7,
    "avgResponseTime": 234.5,
    "p95ResponseTime": 450.2
  },
  "componentStatus": {
    "database": "poor",
    "kafka": "good",
    "businessLogic": "good"
  },
  "alerts": {
    "bottleneck": "Bottleneck detected in: database"
  },
  "slowestOperation": {
    "operation": "AccountRepository.findByIdForUpdate",
    "timeMs": 1250
  },
  "refreshInterval": 30
}
```

### System Load Status
```bash
GET /api/gateway/load-status
```

**Response**:
```json
{
  "data": {
    "cpuUsage": 45.67,
    "activeConnections": 23,
    "processingMode": "SYNC",
    "timestamp": "2024-01-15T10:30:45.123"
  },
  "message": "System load status retrieved successfully",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

## 🏥 Health Checks

### Application Health
```bash
GET /actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "kafka": {
      "status": "UP"
    },
    "datadog": {
      "status": "UP",
      "details": {
        "metricsExport": "enabled",
        "tracingEnabled": "true"
      }
    }
  }
}
```

### Performance Health Check
```bash
GET /api/performance/health/performance
```

**Response**:
```json
{
  "tps": {
    "status": "healthy",
    "value": 45.2,
    "limit": 1000
  },
  "responseTime": {
    "status": "healthy",
    "value": 234.5,
    "limit": 500
  },
  "bottlenecks": {
    "status": "warning",
    "value": "database"
  },
  "overall": {
    "status": "degraded",
    "score": 66
  }
}
```

## 📊 Business Metrics

### Banking-Specific Metrics
```
# Account Operations
banking.accounts.created               # Total accounts created
banking.accounts.active               # Currently active accounts
banking.balance.total                 # Total system balance

# Transaction Metrics  
banking.transactions.success          # Successful transactions
banking.transactions.failure          # Failed transactions
banking.operations.credit             # Credit operations
banking.operations.debit              # Debit operations

# Transfer & Saga Metrics
banking.transfers.initiated           # Transfer requests
banking.transfers.completed           # Completed transfers
banking.transfers.failed             # Failed transfers
banking.saga.compensated             # Compensated transactions
```

### System Metrics (Automatic via Micrometer)
- **JVM Metrics**: Heap usage, garbage collection, thread count
- **Tomcat Metrics**: HTTP connections, thread pools
- **PostgreSQL Metrics**: Connection pool, query execution
- **Kafka Metrics**: Producer/consumer metrics, topic statistics
- **Resilience4j Metrics**: Circuit breaker state, retry attempts

## 🔧 Configuration

### Performance Monitoring
```properties
# Enable performance monitoring
performance.monitoring.enabled=true
performance.monitoring.slow-operation-threshold=1000          # 1 second
performance.monitoring.very-slow-operation-threshold=5000     # 5 seconds
performance.monitoring.throughput-window-seconds=60           # TPS calculation window

# Performance alert thresholds
performance.alerts.tps-warning-threshold=800
performance.alerts.tps-critical-threshold=1000
performance.alerts.response-time-warning-threshold=400        # 400ms
performance.alerts.response-time-critical-threshold=1000      # 1 second
```

### Datadog Configuration
```properties
# Datadog metrics export
management.metrics.export.datadog.enabled=true
management.metrics.export.datadog.api-key=c576fa9380cc70a22dee72e7df176697
management.metrics.export.datadog.site=us5.datadoghq.com
management.metrics.export.datadog.step=60s

# Datadog tags
datadog.environment=production
datadog.service=banking-system
datadog.version=1.0.0
```

### Logging Configuration
```xml
<!-- logback-spring.xml -->
<springProfile name="datadog">
    <appender name="DATADOG" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <pattern>
                    <pattern>
                        {
                            "dd.service": "banking-system",
                            "dd.env": "production",
                            "dd.version": "1.0.0",
                            "dd.trace_id": "%X{dd.trace_id:-}",
                            "dd.span_id": "%X{dd.span_id:-}"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
    </appender>
</springProfile>
```

## 🚨 Alerting System

### Alert Criteria

#### Performance Alerts
1. **High TPS**: 
   - ⚠️ Warning: > 800 TPS
   - 🚨 Critical: > 1000 TPS

2. **Slow Response Time**:
   - ⚠️ Warning: > 400ms average
   - 🚨 Critical: > 1000ms average

3. **Bottleneck Detection**:
   - ⚠️ Warning: Operation > 1 second
   - 🚨 Critical: Operation > 5 seconds

#### Business Alerts
1. **Transaction Failure Rate**: > 5% in 5 minutes
2. **Account Creation Failures**: > 10 failures in 10 minutes
3. **Database Connection Pool**: > 80% utilization

### Alert Log Format
```json
{
  "level": "WARN",
  "message": "Slow database operation: AccountRepository.findByIdForUpdate took 1250ms",
  "component": "database",
  "operation": "AccountRepository.findByIdForUpdate",
  "duration": 1250,
  "threshold": 1000,
  "alertType": "PERFORMANCE_DEGRADATION",
  "severity": "HIGH"
}
```

## 📈 Dashboards & Visualization

### Prometheus Integration
All metrics are exported to Prometheus format:

```bash
curl http://localhost:8080/actuator/prometheus | grep banking
```

**Key Prometheus Metrics**:
```
# HELP banking_requests_total Total requests received
# TYPE banking_requests_total counter
banking_requests_total 1247.0

# HELP banking_request_duration_seconds Request duration
# TYPE banking_request_duration_seconds summary
banking_request_duration_seconds_count 1247.0
banking_request_duration_seconds_sum 295.234
banking_request_duration_seconds{quantile="0.95"} 0.45
banking_request_duration_seconds{quantile="0.99"} 0.89

# HELP banking_throughput_requests_per_second Current TPS
# TYPE banking_throughput_requests_per_second gauge
banking_throughput_requests_per_second 45.2
```

### Grafana Queries
```promql
# TPS (Transactions Per Second)
rate(banking_requests_total[1m])

# Average Response Time
rate(banking_request_duration_seconds_sum[5m]) / rate(banking_request_duration_seconds_count[5m])

# P95 Response Time
histogram_quantile(0.95, rate(banking_request_duration_seconds_bucket[5m]))

# Error Rate
rate(banking_transactions_failure[5m]) / rate(banking_transactions_total[5m]) * 100
```

### Datadog Dashboards
Available in Datadog UI:
- **APM Service Map**: Visual service topology
- **Banking Operations**: Business metric dashboard
- **Infrastructure Overview**: System health dashboard
- **Error Tracking**: Error analysis and trends

## 🔍 Distributed Tracing

### Automatic Instrumentation
- **HTTP Requests**: All REST endpoints traced
- **Database Operations**: PostgreSQL query tracing
- **Kafka Operations**: Message production/consumption
- **Service Calls**: Inter-service communication

### Custom Tracing
```java
// Business operation tracing
@Autowired
private DatadogTracingService tracingService;

public Account createAccount(AccountCreationRequest request) {
    Span span = tracingService.startAccountCreation(request.getCpf());
    try {
        Account account = domainService.createAccount(request);
        span.tag("account.id", account.getId().toString());
        span.tag("account.status", "created");
        return account;
    } catch (Exception e) {
        tracingService.finishSpanWithError(span, e);
        throw e;
    } finally {
        tracingService.finishSpan(span);
    }
}
```

## 🚀 Getting Started

### Local Development
```bash
# Start application with monitoring
mvn spring-boot:run -Dspring-boot.run.profiles=development,monitoring

# Check performance metrics
curl http://localhost:8080/api/performance/dashboard

# View health status
curl http://localhost:8080/actuator/health
```

### Production Setup
```bash
# Set environment variables
export DD_API_KEY="your-datadog-api-key"
export DD_SERVICE="banking-system"
export DD_ENV="production"

# Start with Datadog APM
./start-with-datadog.sh
```

### Monitoring Validation
```bash
# Verify metrics collection
curl http://localhost:8080/actuator/metrics | grep banking

# Check Datadog connectivity
curl http://localhost:8080/actuator/health | grep datadog

# Test performance endpoints
curl http://localhost:8080/api/performance/stats
curl http://localhost:8080/api/performance/bottlenecks
```

## 🎯 Benefits

### ✅ Complete Observability
- Real-time performance metrics
- Distributed tracing across all components
- Business-specific operational metrics
- Automatic bottleneck detection

### ✅ Proactive Issue Detection
- Performance degradation alerts
- Automatic threshold monitoring
- Predictive bottleneck identification
- Error correlation and tracking

### ✅ Operational Excellence
- Data-driven optimization decisions
- SLA compliance monitoring
- Capacity planning insights
- Incident response acceleration

### ✅ Developer Experience
- Detailed performance debugging
- Structured logging with correlation
- Automated metric collection
- Easy-to-use monitoring APIs

The monitoring system provides comprehensive visibility into both technical performance and business operations, enabling proactive issue resolution and continuous optimization.