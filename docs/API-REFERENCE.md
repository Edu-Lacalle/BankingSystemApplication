# 📚 Banking System - API Reference

## Base Information

- **Base URL**: `http://localhost:8080`
- **API Version**: 1.0
- **Content-Type**: `application/json`
- **Authentication**: None (development environment)
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## Response Format

All API responses follow a standardized format with proper HTTP status codes and structured JSON responses.

### Success Response Structure
```json
{
  "data": { /* Response data */ },
  "message": "Operation completed successfully",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

### Error Response Structure
```json
{
  "error": "ERROR_CODE",
  "message": "Human readable error message",
  "timestamp": "2024-01-15T10:30:45.123",
  "path": "/api/gateway/accounts",
  "requestId": "req_abc123def456",
  "details": {
    "field": "error detail"
  }
}
```

## 🚦 API Gateway Endpoints (Intelligent Routing)

The API Gateway automatically routes requests based on system load:
- **Normal Load**: Synchronous processing (immediate response)
- **High Load**: Asynchronous processing (202 Accepted with tracking)

### Create Account
Creates a new bank account with intelligent routing.

**Endpoint**: `POST /api/gateway/accounts`

**Request Body**:
```json
{
  "name": "João Silva",
  "cpf": "12345678901",
  "birthDate": "1990-01-15",
  "email": "joao@email.com",
  "phone": "11987654321"
}
```

**Field Validation**:
- `name`: Required, 2-255 characters
- `cpf`: Required, exactly 11 digits, unique
- `birthDate`: Required, ISO date format (YYYY-MM-DD)
- `email`: Optional, valid email format
- `phone`: Optional, 10-11 digits

**Success Response (Sync - 201 Created)**:
```json
{
  "data": {
    "id": 1,
    "name": "João Silva",
    "cpf": "12345678901",
    "birthDate": "1990-01-15",
    "balance": 0.00,
    "email": "joao@email.com",
    "phone": "11987654321",
    "version": 0
  },
  "message": "Conta criada com sucesso",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

**Success Response (Async - 202 Accepted)**:
```json
{
  "data": {
    "requestId": "req_abc123def456",
    "status": "PROCESSING",
    "estimatedTime": "30-60 seconds"
  },
  "message": "Solicitação aceita para processamento assíncrono",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

**Error Response (409 Conflict)**:
```json
{
  "error": "DUPLICATE_ACCOUNT",
  "message": "Já existe uma conta cadastrada com o CPF: 12345678901",
  "timestamp": "2024-01-15T10:30:45.123",
  "path": "/api/gateway/accounts",
  "requestId": "req_abc123def456"
}
```

### Credit Account
Performs a credit transaction on an account.

**Endpoint**: `POST /api/gateway/transactions/credit`

**Request Body**:
```json
{
  "accountId": 1,
  "amount": 100.50
}
```

**Field Validation**:
- `accountId`: Required, positive integer
- `amount`: Required, positive decimal (min: 0.01)

**Success Response (Sync - 200 OK)**:
```json
{
  "data": {
    "status": "EFETUADO",
    "message": "Crédito efetuado com sucesso",
    "accountId": 1,
    "amount": 100.50,
    "newBalance": 100.50
  },
  "message": "Operação de crédito processada com sucesso",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

**Success Response (Async - 202 Accepted)**:
```json
{
  "data": {
    "requestId": "req_abc123def456",
    "accountId": 1,
    "amount": 100.50,
    "status": "PROCESSING"
  },
  "message": "Operação de crédito aceita para processamento assíncrono",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

### Debit Account
Performs a debit transaction on an account.

**Endpoint**: `POST /api/gateway/transactions/debit`

**Request Body**:
```json
{
  "accountId": 1,
  "amount": 50.25
}
```

**Success Response (Sync - 200 OK)**:
```json
{
  "data": {
    "status": "EFETUADO",
    "message": "Débito efetuado com sucesso",
    "accountId": 1,
    "amount": 50.25,
    "newBalance": 50.25
  },
  "message": "Operação de débito processada com sucesso",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

**Error Response (422 Unprocessable Entity)**:
```json
{
  "error": "INSUFFICIENT_FUNDS",
  "message": "Saldo insuficiente para a operação. Saldo atual: R$ 25.00",
  "timestamp": "2024-01-15T10:30:45.123",
  "path": "/api/gateway/transactions/debit",
  "requestId": "req_abc123def456"
}
```

### Get Account
Retrieves account information by ID.

**Endpoint**: `GET /api/gateway/accounts/{id}`

**Path Parameters**:
- `id`: Account ID (required, positive integer)

**Success Response (200 OK)**:
```json
{
  "data": {
    "id": 1,
    "name": "João Silva",
    "cpf": "12345678901",
    "birthDate": "1990-01-15",
    "balance": 50.25,
    "email": "joao@email.com",
    "phone": "11987654321",
    "version": 2
  },
  "message": "Conta encontrada com sucesso",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

**Error Response (404 Not Found)**:
```json
{
  "error": "ACCOUNT_NOT_FOUND",
  "message": "Conta não encontrada com ID: 999",
  "timestamp": "2024-01-15T10:30:45.123",
  "path": "/api/gateway/accounts/999",
  "requestId": "req_abc123def456"
}
```

### System Load Status
Retrieves current system load and processing mode.

**Endpoint**: `GET /api/gateway/load-status`

**Success Response (200 OK)**:
```json
{
  "data": {
    "cpuUsage": 45.67,
    "activeConnections": 23,
    "processingMode": "SYNC",
    "timestamp": "2024-01-15T10:30:45.123"
  },
  "message": "Status da carga do sistema obtido com sucesso",
  "timestamp": "2024-01-15T10:30:45.123",
  "requestId": "req_abc123def456",
  "status": "SUCCESS"
}
```

## 🔄 Direct API Endpoints (Synchronous)

These endpoints always process requests synchronously, regardless of system load.

### Create Account (Sync)
**Endpoint**: `POST /api/accounts`

### Credit Account (Sync)
**Endpoint**: `POST /api/accounts/credit`

### Debit Account (Sync)
**Endpoint**: `POST /api/accounts/debit`

### Get Account (Sync)
**Endpoint**: `GET /api/accounts/{id}`

*Request/Response formats are identical to Gateway endpoints, but always return immediate responses.*

## ⚡ Asynchronous API Endpoints

These endpoints always process requests asynchronously via Kafka.

### Create Account (Async)
**Endpoint**: `POST /api/accounts/async`

### Credit Account (Async)
**Endpoint**: `POST /api/accounts/async/credit`

### Debit Account (Async)
**Endpoint**: `POST /api/accounts/async/debit`

**Response (202 Accepted)**:
```json
{
  "message": "Request accepted for async processing. Check status endpoint.",
  "requestId": "req_abc123def456",
  "estimatedProcessingTime": "30-60 seconds"
}
```

## 📊 Performance & Monitoring Endpoints

### Performance Dashboard
**Endpoint**: `GET /api/performance/dashboard`

**Success Response (200 OK)**:
```json
{
  "systemMetrics": {
    "cpuUsage": 45.67,
    "memoryUsage": 67.34,
    "activeConnections": 23
  },
  "businessMetrics": {
    "totalAccounts": 150,
    "totalTransactions": 1247,
    "averageTransactionTime": "120ms"
  },
  "timestamp": "2024-01-15T10:30:45.123"
}
```

### System Bottlenecks
**Endpoint**: `GET /api/performance/bottlenecks`

### Performance Statistics
**Endpoint**: `GET /api/performance/stats`

## 🏥 Health & Actuator Endpoints

### Application Health
**Endpoint**: `GET /actuator/health`

**Success Response (200 OK)**:
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
    }
  }
}
```

### System Metrics
**Endpoint**: `GET /actuator/metrics`

### Prometheus Metrics
**Endpoint**: `GET /actuator/prometheus`

## ❌ Error Codes Reference

### Client Errors (4xx)

| Code | Error | Description | HTTP Status |
|------|-------|-------------|-------------|
| `VALIDATION_ERROR` | Invalid input data | Field validation failed | 400 |
| `MALFORMED_JSON` | Invalid JSON format | Request body syntax error | 400 |
| `INVALID_ARGUMENT` | Invalid argument | Business rule validation failed | 400 |
| `ACCOUNT_NOT_FOUND` | Account not found | Account ID doesn't exist | 404 |
| `DUPLICATE_ACCOUNT` | Account already exists | CPF already registered | 409 |
| `CONCURRENT_MODIFICATION` | Optimistic lock failure | Resource modified by another process | 412 |
| `INSUFFICIENT_FUNDS` | Insufficient funds | Account balance too low for debit | 422 |
| `BUSINESS_RULE_VIOLATION` | Business rule violation | Generic business logic error | 422 |

### Server Errors (5xx)

| Code | Error | Description | HTTP Status |
|------|-------|-------------|-------------|
| `INTERNAL_SERVER_ERROR` | Internal server error | Unexpected system error | 500 |
| `DATABASE_ERROR` | Database error | Database connection/query error | 500 |
| `KAFKA_ERROR` | Message broker error | Kafka connection/publishing error | 500 |

## 📝 Request Examples

### cURL Examples

#### Create Account via Gateway
```bash
curl -X POST http://localhost:8080/api/gateway/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Santos",
    "cpf": "98765432100",
    "birthDate": "1985-03-20",
    "email": "maria@email.com",
    "phone": "11999888777"
  }'
```

#### Credit Account
```bash
curl -X POST http://localhost:8080/api/gateway/transactions/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 500.00
  }'
```

#### Debit Account
```bash
curl -X POST http://localhost:8080/api/gateway/transactions/debit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 100.00
  }'
```

#### Get Account
```bash
curl http://localhost:8080/api/gateway/accounts/1
```

#### Check System Load
```bash
curl http://localhost:8080/api/gateway/load-status
```

### JavaScript Examples

#### Create Account
```javascript
const response = await fetch('http://localhost:8080/api/gateway/accounts', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    name: 'João Silva',
    cpf: '12345678901',
    birthDate: '1990-01-15',
    email: 'joao@email.com',
    phone: '11987654321'
  })
});

const result = await response.json();
console.log(result);
```

#### Handle Response
```javascript
if (response.status === 201) {
  // Synchronous processing - account created immediately
  console.log('Account created:', result.data);
} else if (response.status === 202) {
  // Asynchronous processing - check status later
  console.log('Request accepted for processing:', result.data.requestId);
} else {
  // Error occurred
  console.error('Error:', result.error, result.message);
}
```

## 🔧 Configuration Parameters

### Load Balancing Thresholds
- **CPU Threshold**: 70% (switches to async when exceeded)
- **Connection Threshold**: 100 active connections
- **Processing Mode**: AUTO (based on load) / SYNC / ASYNC

### Validation Rules
- **CPF**: Exactly 11 digits, numeric only, unique
- **Email**: RFC 5322 compliant format
- **Phone**: 10-11 digits, numeric only
- **Amount**: Positive decimal, minimum 0.01, maximum 999999.99
- **Name**: 2-255 characters, non-empty

### Rate Limiting
- **Default**: 100 requests per minute per IP
- **Burst**: 20 requests per second
- **Gateway**: 200 requests per minute (higher limit)

## 📈 Performance Characteristics

### Response Times (Typical)
- **Account Creation**: 50-150ms (sync), 10-30ms (async acceptance)
- **Transactions**: 30-80ms (sync), 5-15ms (async acceptance)
- **Account Query**: 10-30ms
- **Load Status**: 5-10ms

### Throughput Capacity
- **Synchronous**: ~500 req/sec (depending on system load)
- **Asynchronous**: ~2000 req/sec (acceptance rate)
- **Mixed Load**: Automatic scaling based on system metrics

---

For complete interactive documentation with request/response examples, visit the [Swagger UI](http://localhost:8080/swagger-ui.html) when the application is running.