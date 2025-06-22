# 🏦 Banking System - Execução Local

## 📋 Pré-requisitos

### Dependências Obrigatórias
- **Docker & Docker Compose** (versão 20.0+)
- **Java 17** (para desenvolvimento local)
- **Maven 3.8+** (para desenvolvimento local)
- **Git** (para clonar o repositório)

### Verificação das Dependências
```bash
# Verificar Docker
docker --version
docker-compose --version

# Verificar Java (opcional para desenvolvimento)
java -version
mvn --version
```

## 🚀 Execução Rápida (Docker Compose)

### Método 1: Script Automático (Recomendado)
```bash
# Clonar o repositório
git clone <repository-url>
cd BankingSystemApplication

# Executar script de início rápido
./quick-start.sh
```

### Método 2: Manual
```bash
# 1. Construir e iniciar todos os serviços
docker-compose up --build -d

# 2. Verificar se os containers estão rodando
docker-compose ps

# 3. Aguardar inicialização (pode levar até 2-3 minutos)
docker-compose logs -f banking-app
```

### 3. Verificar Status
```bash
# Health check da aplicação
curl http://localhost:8080/actuator/health

# Aguardar até ver: {"status":"UP"}
```

## 🛠️ Serviços e Portas

| Serviço | Porta | URL | Descrição |
|---------|-------|-----|-----------|
| **Banking API** | 8080 | http://localhost:8080 | API principal |
| **Swagger UI** | 8080 | http://localhost:8080/swagger-ui.html | Documentação interativa |
| **PostgreSQL** | 5432 | localhost:5432 | Banco de dados |
| **PgAdmin** | 8081 | http://localhost:8081 | Interface do banco |
| **Kafka** | 9092 | localhost:9092 | Message broker |
| **Kafka UI** | 8082 | http://localhost:8082 | Interface do Kafka |
| **Zipkin** | 9411 | http://localhost:9411 | Distributed tracing |
| **Actuator** | 8083 | http://localhost:8083/actuator | Monitoramento |

## 🔐 Credenciais de Acesso

### PostgreSQL
- **Database**: `banking_system`
- **Username**: `banking_user`  
- **Password**: `banking_password`

### PgAdmin
- **Email**: `admin@banking.com`
- **Password**: `admin123`

### Configuração PgAdmin (Primeira vez)
1. Acesse http://localhost:8081
2. Faça login com as credenciais acima
3. Clique em "Add New Server"
4. **General** → Name: `Banking System`
5. **Connection**:
   - Host: `postgres` (nome do container)
   - Port: `5432`
   - Username: `banking_user`
   - Password: `banking_password`

## 📊 Monitoramento e Observabilidade

### Health Checks
```bash
# Status geral da aplicação
curl http://localhost:8080/actuator/health

# Métricas de performance
curl http://localhost:8080/api/performance/stats

# Métricas Prometheus
curl http://localhost:8083/actuator/prometheus
```

### Kafka Monitoring
- **Kafka UI**: http://localhost:8082
- **Tópicos disponíveis**:
  - `banking.account.create`
  - `banking.account.created`
  - `banking.transaction.credit`
  - `banking.transaction.debit`
  - `banking.notifications`

### Distributed Tracing
- **Zipkin**: http://localhost:9411
- Visualizar traces das requisições end-to-end

## 🧪 Testando a Aplicação

### 1. Verificar Status
```bash
curl http://localhost:8080/actuator/health
```

### 2. Criar Conta via API Gateway
```bash
curl -X POST http://localhost:8080/api/gateway/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "cpf": "12345678901",
    "birthDate": "1990-01-15",
    "email": "joao@email.com",
    "phone": "11987654321"
  }'
```

### 3. Realizar Transação
```bash
# Crédito
curl -X POST http://localhost:8080/api/gateway/transactions/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 1000.00
  }'

# Débito
curl -X POST http://localhost:8080/api/gateway/transactions/debit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 100.00
  }'
```

### 4. Consultar Performance
```bash
# Dashboard de performance
curl http://localhost:8080/api/performance/dashboard

# Análise de gargalos
curl http://localhost:8080/api/performance/bottlenecks
```

## 🔄 Desenvolvimento Local (Sem Docker)

### 1. Preparar Dependências Externas
```bash
# Iniciar apenas PostgreSQL e Kafka
docker-compose up -d postgres kafka zookeeper kafka-ui pgadmin zipkin
```

### 2. Configurar Ambiente
```bash
# Exportar variáveis de ambiente
export SPRING_PROFILES_ACTIVE=development
export DATABASE_URL=jdbc:postgresql://localhost:5432/banking_system
export KAFKA_SERVERS=localhost:9092
```

### 3. Executar Aplicação
```bash
# Compilar e executar
mvn clean compile
mvn spring-boot:run

# Ou com Maven Wrapper
./mvnw spring-boot:run
```

## 🧹 Comandos Úteis

### Parar Serviços
```bash
# Parar todos os containers
docker-compose down

# Parar e remover volumes (CUIDADO: apaga dados)
docker-compose down -v
```

### Logs e Debug
```bash
# Ver logs de todos os serviços
docker-compose logs

# Ver logs da aplicação apenas
docker-compose logs -f banking-app

# Ver logs do PostgreSQL
docker-compose logs -f postgres

# Ver logs do Kafka
docker-compose logs -f kafka
```

### Rebuild da Aplicação
```bash
# Rebuild apenas a aplicação
docker-compose build banking-app
docker-compose up -d banking-app

# Rebuild completo
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## 🔧 Troubleshooting

### Problema: Ports em uso
```bash
# Verificar processos usando as portas
netstat -tulpn | grep :8080
netstat -tulpn | grep :5432

# Parar processo específico
kill -9 <PID>
```

### Problema: Containers não sobem
```bash
# Verificar status dos containers
docker-compose ps

# Ver logs de erro
docker-compose logs

# Restart específico
docker-compose restart <service-name>
```

### Problema: Banco não conecta
```bash
# Verificar se PostgreSQL está rodando
docker-compose exec postgres psql -U banking_user -d banking_system -c "SELECT 1;"

# Verificar configurações de rede
docker network ls
docker network inspect bankingsystemapplication_banking_network
```

### Problema: Kafka não funciona
```bash
# Verificar Kafka
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Criar tópico manualmente se necessário
docker-compose exec kafka kafka-topics --create \
  --topic banking.test \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

## 📚 Próximos Passos

1. **Explore a API**: http://localhost:8080/swagger-ui.html
2. **Monitore Performance**: http://localhost:8080/api/performance/dashboard
3. **Visualize Traces**: http://localhost:9411
4. **Analise Kafka**: http://localhost:8082
5. **Gerencie Banco**: http://localhost:8081

## 🆘 Suporte

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8083/actuator/metrics
- **Logs**: `docker-compose logs -f banking-app`
- **Reset Completo**: `docker-compose down -v && docker-compose up -d`