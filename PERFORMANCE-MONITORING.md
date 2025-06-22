# Sistema de Monitoramento de Performance

## 📊 Visão Geral

O sistema bancário agora possui um **sistema completo de monitoramento de performance** que mede e analisa:

✅ **Vazão (Throughput)** - TPS e QPS em tempo real  
✅ **Tempo de Resposta** - Métricas detalhadas com percentis  
✅ **Detecção de Gargalos** - Identificação automática de componentes lentos  
✅ **Dashboard de Performance** - Visualização completa das métricas  

---

## 🎯 Componentes Implementados

### 1. **PerformanceMetricsService** 
📁 `src/main/java/.../metrics/PerformanceMetricsService.java`

**Funcionalidades:**
- **Vazão**: Contabiliza TPS (Transactions Per Second) e QPS (Queries Per Second)
- **Tempo de Resposta**: Métricas de latência com percentis P95 e P99
- **Gargalos**: Detecta operações lentas em Database, Kafka e Business Logic
- **Alertas**: Sistema de thresholds configuráveis

**Métricas Coletadas:**
```java
// Vazão
- banking.requests.total          // Total de requisições
- banking.responses.total         // Total de respostas
- banking.throughput.requests.per.second    // TPS atual
- banking.throughput.transactions.per.second // Transações/segundo

// Tempo de Resposta
- banking.request.duration        // Duração completa da requisição
- banking.response.time          // Tempo de resposta por endpoint
- banking.operation.time         // Tempo por operação

// Gargalos
- banking.bottleneck.database.time   // Tempo de database
- banking.bottleneck.kafka.time     // Tempo de Kafka
- banking.bottleneck.business.time  // Tempo de lógica de negócio
- banking.bottleneck.detected      // Contador de gargalos detectados
```

### 2. **PerformanceInterceptor**
📁 `src/main/java/.../interceptor/PerformanceInterceptor.java`

**Funcionalidades:**
- Intercepta **todas as requisições HTTP**
- Mede **tempo total de processamento**
- Registra **tamanho da resposta**
- Detecta **operações lentas automaticamente**
- Logs estruturados com **MDC context**

**Exemplo de Log:**
```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "message": "Request completed: POST /api/gateway/accounts - 245ms - 200 - 1024bytes",
  "endpoint": "/api/gateway/accounts",
  "method": "POST",
  "duration": "245",
  "status": "success",
  "responseSize": "1024"
}
```

### 3. **PerformanceAspect** 
📁 `src/main/java/.../aspect/PerformanceAspect.java`

**Funcionalidades:**
- **AOP (Aspect-Oriented Programming)** para monitoramento granular
- Mede performance de componentes específicos:
  - `@Around("execution(* ...repository.*.*(..))")` - **Database operations**
  - `@Around("execution(* ...service.kafka.*.*(..))")` - **Kafka operations**
  - `@Around("execution(* ...domain.service.*.*(..))")` - **Business logic**
  - `@Around("execution(* ...messaging.*.*(..))")` - **Messaging operations**

**Detecção Automática:**
```java
if (performanceMetricsService.isSlowOperation(duration)) {
    performanceMetricsService.recordBottleneck("database", operation, duration);
    logger.warn("Slow database operation: {} took {}ms", operation, duration.toMillis());
}
```

### 4. **PerformanceController** 
📁 `src/main/java/.../controller/PerformanceController.java`

**Endpoints de Monitoramento:**

#### 📈 **Estatísticas Completas**
```bash
GET /api/performance/stats
```
**Resposta:**
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

#### 🚀 **Métricas de Vazão**
```bash
GET /api/performance/throughput
```
**Resposta:**
```json
{
  "currentTPS": 45.2,
  "currentTransactionTPS": 38.7,
  "description": {
    "currentTPS": "Requisições por segundo",
    "currentTransactionTPS": "Transações por segundo"
  },
  "timestamp": 1705312200000
}
```

#### ⏱️ **Tempo de Resposta**
```bash
GET /api/performance/response-time
```
**Resposta:**
```json
{
  "averageResponseTime": 234.5,
  "p95ResponseTime": 450.2,
  "p99ResponseTime": 890.1,
  "unit": "milliseconds",
  "description": {
    "averageResponseTime": "Tempo médio de resposta",
    "p95ResponseTime": "95% das requisições são atendidas em até",
    "p99ResponseTime": "99% das requisições são atendidas em até"
  }
}
```

#### 🔍 **Detecção de Gargalos**
```bash
GET /api/performance/bottlenecks
```
**Resposta:**
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
    "database": "Considere otimizar queries, adicionar índices ou aumentar pool de conexões"
  }
}
```

#### 🏥 **Health Check de Performance**
```bash
GET /api/performance/health/performance
```
**Resposta:**
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

#### 📊 **Dashboard Completo**
```bash
GET /api/performance/dashboard
```
**Resposta:**
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
    "bottleneck": "Gargalo detectado em: database"
  },
  "slowestOperation": {
    "operation": "AccountRepository.findByIdForUpdate",
    "timeMs": 1250
  },
  "refreshInterval": 30
}
```

---

## ⚙️ Configurações

### **application.properties**

```properties
# Performance monitoring configuration
performance.monitoring.enabled=true
performance.monitoring.slow-operation-threshold=1000          # 1 segundo
performance.monitoring.very-slow-operation-threshold=5000     # 5 segundos
performance.monitoring.throughput-window-seconds=60           # Janela de cálculo TPS

# Performance thresholds for alerts
performance.alerts.tps-warning-threshold=800
performance.alerts.tps-critical-threshold=1000
performance.alerts.response-time-warning-threshold=400        # 400ms
performance.alerts.response-time-critical-threshold=1000      # 1 segundo
```

### **Configuração de Interceptors**
📁 `src/main/java/.../config/PerformanceConfig.java`

```java
@Configuration
@EnableAspectJAutoProxy
public class PerformanceConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/performance/**", "/actuator/**");
    }
}
```

---

## 📋 Como Usar

### 1. **Monitoramento em Tempo Real**

```bash
# Verificar TPS atual
curl http://localhost:8080/api/performance/throughput

# Verificar tempo de resposta
curl http://localhost:8080/api/performance/response-time

# Verificar gargalos
curl http://localhost:8080/api/performance/bottlenecks
```

### 2. **Dashboard Completo**

```bash
# Obter todas as métricas para dashboard
curl http://localhost:8080/api/performance/dashboard
```

### 3. **Health Check de Performance**

```bash
# Verificar saúde da performance
curl http://localhost:8080/api/performance/health/performance
```

### 4. **Integração com Prometheus**

Todas as métricas são automaticamente expostas via Micrometer:

```bash
# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus | grep banking
```

**Métricas Principais:**
```
# HELP banking_requests_total Total de requisições recebidas
# TYPE banking_requests_total counter
banking_requests_total 1247.0

# HELP banking_request_duration_seconds Duração completa da requisição
# TYPE banking_request_duration_seconds summary
banking_request_duration_seconds_count 1247.0
banking_request_duration_seconds_sum 295.234
banking_request_duration_seconds{quantile="0.95"} 0.45
banking_request_duration_seconds{quantile="0.99"} 0.89

# HELP banking_throughput_requests_per_second Requisições por segundo atual
# TYPE banking_throughput_requests_per_second gauge
banking_throughput_requests_per_second 45.2
```

---

## 🚨 Sistema de Alertas

### **Critérios de Alerta:**

1. **TPS Alto**: 
   - ⚠️ Warning: > 800 TPS
   - 🚨 Critical: > 1000 TPS

2. **Tempo de Resposta Lento**:
   - ⚠️ Warning: > 400ms
   - 🚨 Critical: > 1000ms

3. **Gargalos Detectados**:
   - ⚠️ Warning: Operação > 1 segundo
   - 🚨 Critical: Operação > 5 segundos

### **Logs de Alerta:**

```json
{
  "level": "WARN",
  "message": "Slow database operation: AccountRepository.findByIdForUpdate took 1250ms",
  "component": "database",
  "operation": "AccountRepository.findByIdForUpdate",
  "duration": 1250,
  "threshold": 1000
}
```

---

## 📈 Monitoramento com Grafana

### **Queries Prometheus para Grafana:**

```promql
# TPS
rate(banking_requests_total[1m])

# Tempo de resposta médio
rate(banking_request_duration_seconds_sum[5m]) / rate(banking_request_duration_seconds_count[5m])

# P95 tempo de resposta
histogram_quantile(0.95, rate(banking_request_duration_seconds_bucket[5m]))

# Gargalos por componente
banking_bottleneck_database_time
banking_bottleneck_kafka_time
banking_bottleneck_business_time
```

---

## 🔧 Extensões Futuras

### **Possíveis Melhorias:**

1. **Machine Learning**: Detecção predictiva de gargalos
2. **Auto-scaling**: Integração com Kubernetes HPA
3. **Alerting**: Integração com sistemas como PagerDuty
4. **Profiling**: Integração com ferramentas como Java Flight Recorder
5. **Caching**: Métricas de cache hit/miss ratio

---

## 🎯 Benefícios Implementados

✅ **Visibilidade Completa**: Métricas em tempo real de todos os componentes  
✅ **Detecção Proativa**: Identificação automática de problemas de performance  
✅ **Debugging Facilitado**: Logs estruturados com correlação  
✅ **Otimização Orientada**: Dados concretos para melhorias  
✅ **SLA Monitoring**: Acompanhamento de SLAs de performance  
✅ **Escalabilidade**: Base sólida para crescimento do sistema  

O sistema agora oferece **observabilidade completa** de performance, permitindo identificar e resolver gargalos proativamente.