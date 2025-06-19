# Banking System API

Sistema bancário desenvolvido em Spring Boot com PostgreSQL para gerenciamento de contas e transações.

## 🏗️ Arquitetura

- **Backend**: Spring Boot 3.1.5
- **Banco de Dados**: PostgreSQL 15
- **Migração**: Flyway
- **Documentação**: OpenAPI/Swagger
- **Container**: Docker Compose

## 🚀 Como Executar

### Pré-requisitos
- Java 17+
- Docker e Docker Compose
- Maven 3.6+

### 1. Subir a Infraestrutura (Kafka + PostgreSQL)
```bash
docker-compose up -d
```

### 2. Executar a Aplicação
```bash
mvn spring-boot:run
```

### 3. Acessar os Recursos
- **API**: http://localhost:8080/api
- **API Assíncrona**: http://localhost:8080/api/accounts/async
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Kafka UI**: http://localhost:8082
- **PgAdmin**: http://localhost:8081 (admin@banking.com / admin123)

## 🐳 Docker Compose

O arquivo `docker-compose.yml` inclui:

### Apache Kafka
- **Porta**: 9092
- **Zookeeper**: 2181
- **Kafka UI**: http://localhost:8082
- **Tópicos**: banking-transactions, banking-notifications, banking-audit

### PostgreSQL
- **Porta**: 5432
- **Banco**: banking_system
- **Usuário**: banking_user
- **Senha**: banking_password

### PgAdmin (Interface Gráfica)
- **Porta**: 8081
- **Email**: admin@banking.com
- **Senha**: admin123

## 📊 Estrutura do Banco

### Tabela: accounts
```sql
- id (BIGSERIAL PRIMARY KEY)
- name (VARCHAR(255) NOT NULL)
- cpf (VARCHAR(11) UNIQUE NOT NULL)
- birth_date (DATE NOT NULL)
- balance (DECIMAL(19,2) DEFAULT 0.00)
- email (VARCHAR(255))
- phone (VARCHAR(11))
- version (BIGINT DEFAULT 0) -- Controle de concorrência
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

### Índices
- `idx_accounts_cpf` - Para busca rápida por CPF
- `idx_accounts_email` - Para busca por email

## 🔧 Configuração

### application.properties
```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/banking_system
spring.datasource.username=banking_user
spring.datasource.password=banking_password

# Flyway (Migração)
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

## 📋 Endpoints da API

### API Síncrona (/api/accounts)

#### 1. Criar Conta
```bash
POST /api/accounts
Content-Type: application/json

{
  "name": "João Silva",
  "cpf": "12345678901",
  "birthDate": "1990-01-15",
  "email": "joao@email.com",
  "phone": "11987654321"
}
```

#### 2. Creditar Conta
```bash
POST /api/accounts/credit
Content-Type: application/json

{
  "accountId": 1,
  "amount": 100.50
}
```

#### 3. Debitar Conta
```bash
POST /api/accounts/debit
Content-Type: application/json

{
  "accountId": 1,
  "amount": 50.25
}
```

#### 4. Consultar Conta
```bash
GET /api/accounts/{id}
```

### API Assíncrona (/api/accounts/async)

#### 1. Criar Conta (Assíncrona)
```bash
POST /api/accounts/async
Content-Type: application/json

{
  "name": "Maria Santos",
  "cpf": "98765432100",
  "birthDate": "1985-03-20",
  "email": "maria@email.com",
  "phone": "11999888777"
}
```
*Publica eventos: auditoria + notificação de boas-vindas*

#### 2. Creditar Conta (Assíncrona)
```bash
POST /api/accounts/async/credit
Content-Type: application/json

{
  "accountId": 1,
  "amount": 500.00
}
```
*Publica eventos: transação + auditoria + notificação*

#### 3. Debitar Conta (Assíncrona)
```bash
POST /api/accounts/async/debit
Content-Type: application/json

{
  "accountId": 1,
  "amount": 100.00
}
```
*Publica eventos: transação + auditoria + notificação*

## 🔄 Migrações Flyway

As migrações estão em `src/main/resources/db/migration/`:

- `V1__Create_accounts_table.sql` - Criação da tabela principal

Para executar migrações manualmente:
```bash
mvn flyway:migrate
```

## ⚡ Apache Kafka - Processamento Assíncrono

### Tópicos Configurados
- **banking-transactions**: Eventos de transações (crédito/débito)
- **banking-notifications**: Eventos para notificações por email
- **banking-audit**: Eventos para auditoria e compliance

### Producers
- **TransactionEventProducer**: Publica eventos de transações e auditoria
- Eventos incluem: ID único, conta, valor, tipo, status e timestamp

### Consumers
- **TransactionEventConsumer**: Processa eventos de transações e auditoria
- **NotificationEventConsumer**: Processa notificações (boas-vindas, confirmações, falhas)

### Casos de Uso Assíncronos
1. **Criação de Conta**: 
   - Evento de auditoria para compliance
   - Email de boas-vindas automaticamente

2. **Transações**:
   - Log detalhado para auditoria
   - Notificações de confirmação/falha
   - Processamento de regras de negócio

3. **Monitoramento**:
   - Análise de padrões de transação
   - Detecção de fraudes
   - Relatórios em tempo real

### Kafka UI
Acesse http://localhost:8082 para:
- Visualizar tópicos e mensagens
- Monitorar consumers e lag
- Debug de eventos em tempo real

## 🛡️ Recursos de Segurança

### Controle de Concorrência
- Utiliza `@Version` para controle otimista
- Previne condições de corrida em transações

### Validações
- CPF único e obrigatório (11 dígitos)
- Email com formato válido
- Valores monetários positivos
- Dados obrigatórios validados

### Auditoria
- Campos `created_at` e `updated_at` automáticos
- Triggers PostgreSQL para timestamp
- Logs detalhados de transações

## 🧪 Testes

### Executar Testes
```bash
mvn test
```

### Teste Manual via cURL

#### API Síncrona
```bash
# Criar conta
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","cpf":"12345678901","birthDate":"1990-01-01"}'

# Creditar
curl -X POST http://localhost:8080/api/accounts/credit \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"amount":100.00}'
```

#### API Assíncrona (com eventos Kafka)
```bash
# Criar conta assíncrona
curl -X POST http://localhost:8080/api/accounts/async \
  -H "Content-Type: application/json" \
  -d '{"name":"Async User","cpf":"11122233344","birthDate":"1995-05-15","email":"async@email.com"}'

# Creditar assíncrono
curl -X POST http://localhost:8080/api/accounts/async/credit \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"amount":250.00}'
```

*Após usar a API assíncrona, verifique os eventos no Kafka UI (http://localhost:8082)*

## 📝 Monitoramento

### Logs
- Nível DEBUG para pacote `com.bank`
- Logs SQL formatados
- Timezone UTC configurado

### Métricas
- Controle de versão da entidade
- Timestamps de criação/atualização
- Status de transações (EFETUADO/RECUSADO)

## 🔧 Comandos Úteis

### Docker
```bash
# Subir todos os serviços (Kafka + PostgreSQL)
docker-compose up -d

# Ver logs do Kafka
docker-compose logs kafka

# Ver logs do PostgreSQL
docker-compose logs postgres

# Acessar PostgreSQL via terminal
docker exec -it banking_postgres psql -U banking_user -d banking_system

# Acessar Kafka via terminal
docker exec -it banking_kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic banking-transactions --from-beginning

# Parar serviços
docker-compose down
```

### Maven
```bash
# Compilar
mvn compile

# Executar testes
mvn test

# Executar aplicação
mvn spring-boot:run

# Limpar e compilar
mvn clean install
```

## 📚 Documentação Adicional

- [API-DOCUMENTACAO.md](./API-DOCUMENTACAO.md) - Documentação completa da API
- [Swagger UI](http://localhost:8080/swagger-ui.html) - Documentação interativa
- [Flyway Docs](https://flywaydb.org/documentation/) - Documentação do Flyway

## 🚨 Resolução de Problemas

### Banco não conecta
1. Verificar se PostgreSQL está rodando: `docker-compose ps`
2. Verificar logs: `docker-compose logs postgres`
3. Testar conexão: `telnet localhost 5432`

### Migração falha
1. Verificar status: `mvn flyway:info`
2. Limpar e recriar: `docker-compose down -v && docker-compose up -d`

### Aplicação não inicia
1. Verificar Java 17: `java --version`
2. Verificar conexão DB nas configurações
3. Verificar logs da aplicação

