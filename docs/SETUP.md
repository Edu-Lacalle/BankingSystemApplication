# 🚀 Banking System - Setup & Execution Guide

## Prerequisites

### Required Software
- **Docker & Docker Compose** (version 20.0+)
- **Java 17** (for local development)
- **Maven 3.8+** (for local development)
- **Git** (for cloning repository)

### Verification Commands
```bash
# Verify Docker
docker --version
docker-compose --version

# Verify Java (optional for development)
java -version
mvn --version
```

## 🐳 Quick Start with Docker (Recommended)

### Method 1: Automated Script
```bash
# Clone repository
git clone <repository-url>
cd BankingSystemApplication

# Run quick start script (if available)
./quick-start.sh
```

### Method 2: Manual Docker Setup
```bash
# Build and start all services
docker-compose up --build -d

# Verify containers are running
docker-compose ps

# Wait for initialization (2-3 minutes)
docker-compose logs -f banking-app

# Health check
curl http://localhost:8080/actuator/health
# Wait for: {"status":"UP"}
```

## 🛠️ Service Configuration

### Services & Ports
| Service | Port | URL | Credentials |
|---------|------|-----|-------------|
| **Banking API** | 8080 | http://localhost:8080 | - |
| **Swagger UI** | 8080 | http://localhost:8080/swagger-ui.html | - |
| **PostgreSQL** | 5432 | localhost:5432 | `banking_user` / `banking_password` |
| **PgAdmin** | 8081 | http://localhost:8081 | `admin@banking.com` / `admin123` |
| **Kafka** | 9092 | localhost:9092 | - |
| **Kafka UI** | 8082 | http://localhost:8082 | - |
| **Zipkin** | 9411 | http://localhost:9411 | - |
| **Actuator** | 8083 | http://localhost:8083/actuator | - |

### Database Configuration
```properties
# PostgreSQL Settings
Database: banking_system
Username: banking_user
Password: banking_password
URL: jdbc:postgresql://localhost:5432/banking_system
```

### PgAdmin Setup (First Time)
1. Access http://localhost:8081
2. Login: `admin@banking.com` / `admin123`
3. Add New Server:
   - **Name**: `Banking System`
   - **Host**: `postgres` (container name)
   - **Port**: `5432`
   - **Username**: `banking_user`
   - **Password**: `banking_password`

## ⚡ Local Development Setup

### Start Dependencies Only
```bash
# Start PostgreSQL, Kafka, and supporting services
docker-compose up -d postgres kafka zookeeper kafka-ui pgadmin zipkin

# Verify services are up
docker-compose ps
```

### Environment Variables
```bash
# Set development profile
export SPRING_PROFILES_ACTIVE=development
export DATABASE_URL=jdbc:postgresql://localhost:5432/banking_system
export KAFKA_SERVERS=localhost:9092
```

### Run Application Locally
```bash
# Compile and run
mvn clean compile
mvn spring-boot:run

# Or with Maven Wrapper
./mvnw spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=development
```

## 🧪 Verification & Testing

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database connection test
curl http://localhost:8080/api/accounts/1
# Should return 404 Not Found (expected for empty database)

# Performance metrics
curl http://localhost:8080/api/performance/stats
```

### Basic API Testing
```bash
# Create test account
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "cpf": "12345678901",
    "birthDate": "1990-01-01",
    "email": "test@email.com"
  }'

# Credit account (use returned account ID)
curl -X POST http://localhost:8080/api/accounts/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 100.00
  }'

# Check account balance
curl http://localhost:8080/api/accounts/1
```

### Gateway Testing
```bash
# Test intelligent routing through API Gateway
curl -X POST http://localhost:8080/api/gateway/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gateway User",
    "cpf": "98765432100",
    "birthDate": "1995-05-15",
    "email": "gateway@email.com"
  }'

# Check system load status
curl http://localhost:8080/api/gateway/load-status
```

### Async Processing Testing
```bash
# Create account with async processing
curl -X POST http://localhost:8080/api/accounts/async \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Async User",
    "cpf": "11122233344",
    "birthDate": "1992-03-10",
    "email": "async@email.com"
  }'

# Check Kafka events in Kafka UI: http://localhost:8082
```

## 📊 Monitoring Setup

### Datadog Integration (Optional)
If you want to enable Datadog monitoring:

1. **Get Datadog API Key** from your Datadog account
2. **Update Configuration**:
   ```properties
   # application.properties
   management.metrics.export.datadog.api-key=your-api-key-here
   management.metrics.export.datadog.enabled=true
   datadog.site=us5.datadoghq.com
   ```
3. **Start with Datadog Agent**:
   ```bash
   # Run application with Datadog
   ./start-with-datadog.sh
   ```

### Performance Monitoring
```bash
# Performance dashboard
curl http://localhost:8080/api/performance/dashboard

# System bottlenecks analysis
curl http://localhost:8080/api/performance/bottlenecks

# Prometheus metrics
curl http://localhost:8083/actuator/prometheus
```

## 🔧 Development Commands

### Build Commands
```bash
# Full clean build
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests

# Run only tests
mvn test

# Package application
mvn package
```

### Docker Commands
```bash
# View logs
docker-compose logs -f banking-app    # Application logs
docker-compose logs -f postgres       # Database logs
docker-compose logs -f kafka          # Kafka logs

# Restart specific service
docker-compose restart banking-app

# Rebuild specific service
docker-compose build banking-app
docker-compose up -d banking-app

# Stop all services
docker-compose down

# Stop and remove volumes (CAUTION: Deletes data)
docker-compose down -v
```

### Database Commands
```bash
# Access PostgreSQL directly
docker exec -it banking_postgres psql -U banking_user -d banking_system

# SQL Queries
# List all accounts
SELECT * FROM accounts;

# Check database schema
\dt

# Exit PostgreSQL
\q
```

### Kafka Commands
```bash
# Access Kafka container
docker exec -it banking_kafka bash

# List topics
kafka-topics --list --bootstrap-server localhost:9092

# Create topic manually
kafka-topics --create \
  --topic banking.test \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# Consume messages from topic
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic banking.account.create \
  --from-beginning
```

## 🚨 Troubleshooting

### Common Issues & Solutions

#### Port Already in Use
```bash
# Check what's using the port
netstat -tulpn | grep :8080
lsof -i :8080

# Kill process using the port
kill -9 <PID>
```

#### Containers Won't Start
```bash
# Check container status
docker-compose ps

# View error logs
docker-compose logs <service-name>

# Remove and recreate containers
docker-compose down
docker-compose up -d --force-recreate
```

#### Database Connection Issues
```bash
# Test database connection
docker exec -it banking_postgres psql -U banking_user -d banking_system -c "SELECT 1;"

# Check network connectivity
docker network ls
docker network inspect bankingsystemapplication_banking_network

# Reset database completely
docker-compose down -v
docker volume rm $(docker volume ls -q)
docker-compose up -d postgres
```

#### Application Won't Start
```bash
# Check Java version
java --version

# Check if all dependencies are available
docker-compose ps

# View detailed application logs
docker-compose logs -f banking-app

# Check application properties
cat src/main/resources/application.properties
```

#### Kafka Issues
```bash
# Verify Kafka is running
docker-compose logs kafka

# Check if topics exist
docker exec -it banking_kafka kafka-topics --list --bootstrap-server localhost:9092

# Reset Kafka data
docker-compose down
docker volume rm bankingsystemapplication_kafka_data
docker-compose up -d
```

### Reset Everything
```bash
# Nuclear option: Complete reset
docker-compose down -v
docker system prune -a
docker volume prune -f
docker-compose up --build -d
```

## 📋 Production Checklist

Before deploying to production:

### Security
- [ ] Change default passwords
- [ ] Configure proper database credentials
- [ ] Set up SSL/TLS certificates
- [ ] Enable authentication/authorization
- [ ] Configure firewall rules

### Performance
- [ ] Tune JVM parameters
- [ ] Configure connection pools
- [ ] Set proper timeout values
- [ ] Enable caching where appropriate
- [ ] Configure resource limits

### Monitoring
- [ ] Set up log aggregation
- [ ] Configure alerting rules
- [ ] Set up health check endpoints
- [ ] Enable metrics collection
- [ ] Configure backup procedures

### Configuration
- [ ] Use environment-specific properties
- [ ] Externalize sensitive configuration
- [ ] Set up configuration management
- [ ] Document deployment procedures
- [ ] Prepare rollback procedures

## 📞 Getting Help

### Health Check Endpoints
- Application Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8083/actuator/metrics
- API Documentation: http://localhost:8080/swagger-ui.html

### Log Locations
```bash
# Docker logs
docker-compose logs -f banking-app

# Local development logs
tail -f logs/banking-system.log

# Application-specific logs
docker exec -it banking_app tail -f /app/logs/application.log
```

### Support Resources
- Health Check: http://localhost:8080/actuator/health
- Swagger Documentation: http://localhost:8080/swagger-ui.html
- Performance Dashboard: http://localhost:8080/api/performance/dashboard
- System Load Status: http://localhost:8080/api/gateway/load-status

---

For additional help, check the application logs and health endpoints listed above. Most issues can be resolved by restarting services or checking the configuration files.