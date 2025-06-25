# 🏦 Banking System Application

A comprehensive banking system built with Spring Boot 3.1.5, implementing hexagonal architecture for high scalability and maintainability.

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Maven 3.8+

### Run with Docker (Recommended)
```bash
# Start all services
docker-compose up -d

# Verify health
curl http://localhost:8080/actuator/health
```

### Run Locally (Development)
```bash
# Start dependencies only
docker-compose up -d postgres kafka zookeeper

# Run application
mvn spring-boot:run
```

## 🏗️ Architecture

**Hexagonal Architecture** with:
- **Domain**: Core business logic and entities
- **Application**: Use cases and orchestration
- **Infrastructure**: External integrations (DB, Kafka, etc.)
- **Adapters**: Web controllers and external services

**Technology Stack**:
- Spring Boot 3.1.5, Java 17
- PostgreSQL 15 with Flyway migrations
- Apache Kafka for async processing
- Datadog APM for monitoring
- Docker containerization

## 📊 Services & Ports

| Service | Port | URL | Description |
|---------|------|-----|-------------|
| **Banking API** | 8080 | http://localhost:8080 | Main application |
| **Swagger UI** | 8080 | http://localhost:8080/swagger-ui.html | API documentation |
| **PgAdmin** | 8081 | http://localhost:8081 | Database UI |
| **Kafka UI** | 8082 | http://localhost:8082 | Message broker UI |
| **Zipkin** | 9411 | http://localhost:9411 | Distributed tracing |

## 🔌 API Endpoints

### Core Banking Operations
```bash
# Create account
POST /api/accounts
{
  "name": "João Silva",
  "cpf": "12345678901",
  "birthDate": "1990-01-15",
  "email": "joao@email.com"
}

# Credit account
POST /api/accounts/credit
{
  "accountId": 1,
  "amount": 100.50
}

# Debit account
POST /api/accounts/debit
{
  "accountId": 1,
  "amount": 50.25
}

# Get account
GET /api/accounts/{id}
```

### API Gateway (Enhanced)
```bash
# Unified operations through gateway
POST /api/gateway/accounts
POST /api/gateway/transactions/credit
POST /api/gateway/transactions/debit
GET /api/gateway/accounts/{id}
```

## ⚡ Async Processing with Kafka

**Topics**:
- `banking.account.create` - Account creation events
- `banking.transaction.credit` - Credit transactions
- `banking.transaction.debit` - Debit transactions
- `banking.notifications` - Email notifications

**Async Endpoints**:
```bash
POST /api/accounts/async          # Creates account + publishes events
POST /api/accounts/async/credit   # Credits + audit + notification
POST /api/accounts/async/debit    # Debits + audit + notification
```

## 📈 Monitoring & Observability

- **Datadog APM**: Full application monitoring (requires `DD_API_KEY` environment variable)
- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Performance Dashboard**: `/api/performance/dashboard`
- **Distributed Tracing**: Zipkin integration

## 🔒 Security & Validation

- **Optimistic Locking**: Version control for concurrency
- **Input Validation**: CPF uniqueness, email format, positive amounts
- **Structured Error Handling**: Standardized error responses with request tracking
- **HTTP Status Codes**: Proper 4xx/5xx responses for different scenarios

## 🧪 Testing

```bash
# Run tests
mvn test

# Create test account
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","cpf":"12345678901","birthDate":"1990-01-01"}'
```

## 🛠️ Development

```bash
# Build
mvn clean install

# Run with profile
mvn spring-boot:run -Dspring-boot.run.profiles=development

# View logs
docker-compose logs -f banking-app
```

## 📚 Documentation

- **[Setup Guide](docs/SETUP.md)** - Detailed installation and configuration
- **[Architecture](docs/ARCHITECTURE.md)** - In-depth architectural decisions
- **[API Reference](docs/API-REFERENCE.md)** - Complete API documentation
- **[Monitoring](docs/MONITORING.md)** - Performance and Datadog setup
- **[Development](docs/DEVELOPMENT.md)** - Patterns and best practices
- **[Cloud Deployment](docs/CLOUD-DEPLOYMENT.md)** - AWS infrastructure, CI/CD pipeline, and deployment strategies

## 🔧 Configuration

**Database**:
- URL: `jdbc:postgresql://localhost:5432/banking_system`
- User: `banking_user` / Password: `banking_password`

**Kafka**: 
- Brokers: `localhost:9092`
- Auto-topic creation enabled

**Datadog**:
- Site: `us5.datadoghq.com`
- APM enabled with local agent on port 8126

## 🚨 Troubleshooting

```bash
# Reset everything
docker-compose down -v && docker-compose up -d

# Check service health
curl http://localhost:8080/actuator/health

# View application logs
docker-compose logs -f banking-app

# Database connection test
docker exec -it banking_postgres psql -U banking_user -d banking_system
```

## 📞 Support

- Health Check: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- Logs: `docker-compose logs -f banking-app`

---
**Built with Spring Boot 3.1.5 • Hexagonal Architecture • Datadog Monitoring**