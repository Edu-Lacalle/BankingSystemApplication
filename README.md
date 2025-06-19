# API Banking System - Documentação

## Visão Geral
A API Banking System é um sistema de gerenciamento de contas bancárias desenvolvido em Spring Boot. Permite criar contas, realizar operações de crédito e débito, e consultar informações de contas.

## Informações Técnicas
- **Framework**: Spring Boot 3.1.5
- **Java**: 17
- **Banco de Dados**: H2 (em memória)
- **Documentação**: OpenAPI 3.0 (Swagger)
- **Porta**: 8080 (padrão)

## URL Base
```
http://localhost:8080/api
```

## Documentação Swagger
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## Endpoints

### 1. Criar Conta
Cria uma nova conta bancária.

**Endpoint**: `POST /api/accounts`

**Corpo da Requisição**:
```json
{
  "name": "João Silva",
  "cpf": "12345678901",
  "birthDate": "1990-01-15",
  "email": "joao@email.com",
  "phone": "11987654321"
}
```

**Campos Obrigatórios**:
- `name`: Nome completo (obrigatório)
- `cpf`: CPF com 11 dígitos (obrigatório, único)
- `birthDate`: Data de nascimento no formato YYYY-MM-DD (obrigatório)

**Campos Opcionais**:
- `email`: Email válido
- `phone`: Telefone com 10 ou 11 dígitos

**Respostas**:
- **201 Created**: Conta criada com sucesso
- **400 Bad Request**: Dados inválidos
- **500 Internal Server Error**: Erro interno do servidor

**Exemplo de Resposta (201)**:
```json
{
  "id": 1,
  "name": "João Silva",
  "cpf": "12345678901",
  "birthDate": "1990-01-15",
  "balance": 0.00,
  "email": "joao@email.com",
  "phone": "11987654321",
  "version": 0
}
```

### 2. Creditar Conta
Adiciona valor ao saldo de uma conta.

**Endpoint**: `POST /api/accounts/credit`

**Corpo da Requisição**:
```json
{
  "accountId": 1,
  "amount": 100.50
}
```

**Campos**:
- `accountId`: ID da conta (obrigatório)
- `amount`: Valor a ser creditado (obrigatório, deve ser positivo)

**Respostas**:
- **200 OK**: Transação efetuada com sucesso
- **400 Bad Request**: Dados inválidos ou conta não encontrada

**Exemplo de Resposta (200)**:
```json
{
  "status": "EFETUADO",
  "message": "Crédito efetuado com sucesso"
}
```

**Exemplo de Resposta (400)**:
```json
{
  "status": "RECUSADO",
  "message": "Conta não encontrada"
}
```

### 3. Debitar Conta
Remove valor do saldo de uma conta.

**Endpoint**: `POST /api/accounts/debit`

**Corpo da Requisição**:
```json
{
  "accountId": 1,
  "amount": 50.25
}
```

**Campos**:
- `accountId`: ID da conta (obrigatório)
- `amount`: Valor a ser debitado (obrigatório, deve ser positivo)

**Respostas**:
- **200 OK**: Transação efetuada com sucesso
- **400 Bad Request**: Dados inválidos, conta não encontrada ou saldo insuficiente

**Exemplo de Resposta (200)**:
```json
{
  "status": "EFETUADO",
  "message": "Débito efetuado com sucesso"
}
```

**Exemplo de Resposta (400)**:
```json
{
  "status": "RECUSADO",
  "message": "Saldo insuficiente"
}
```

### 4. Consultar Conta
Busca informações de uma conta pelo ID.

**Endpoint**: `GET /api/accounts/{id}`

**Parâmetros**:
- `id`: ID da conta (obrigatório)

**Respostas**:
- **200 OK**: Conta encontrada
- **404 Not Found**: Conta não encontrada
- **500 Internal Server Error**: Erro interno do servidor

**Exemplo de Resposta (200)**:
```json
{
  "id": 1,
  "name": "João Silva",
  "cpf": "12345678901",
  "birthDate": "1990-01-15",
  "balance": 50.25,
  "email": "joao@email.com",
  "phone": "11987654321",
  "version": 2
}
```

## Modelos de Dados

### Account (Conta)
```json
{
  "id": "number",
  "name": "string",
  "cpf": "string",
  "birthDate": "date",
  "balance": "decimal",
  "email": "string",
  "phone": "string",
  "version": "number"
}
```

### AccountCreationRequest
```json
{
  "name": "string (obrigatório)",
  "cpf": "string (obrigatório, 11 dígitos)",
  "birthDate": "date (obrigatório)",
  "email": "string (formato email)",
  "phone": "string (10-11 dígitos)"
}
```

### TransactionRequest
```json
{
  "accountId": "number (obrigatório)",
  "amount": "decimal (obrigatório, positivo)"
}
```

### TransactionResponse
```json
{
  "status": "EFETUADO | RECUSADO",
  "message": "string"
}
```

## Códigos de Status HTTP

### Sucesso
- **200 OK**: Requisição processada com sucesso
- **201 Created**: Recurso criado com sucesso

### Erro do Cliente
- **400 Bad Request**: Dados inválidos na requisição
- **404 Not Found**: Recurso não encontrado

### Erro do Servidor
- **500 Internal Server Error**: Erro interno do servidor

## Validações

### CPF
- Deve conter exatamente 11 dígitos numéricos
- Deve ser único no sistema

### Email
- Deve ter formato válido de email
- Campo opcional

### Telefone
- Deve conter 10 ou 11 dígitos numéricos
- Campo opcional

### Valores Monetários
- Devem ser positivos
- Precisão de 2 casas decimais

## Tratamento de Erros

A API retorna mensagens de erro em português. Exemplos:

```json
{
  "status": "RECUSADO",
  "message": "Conta não encontrada"
}
```

```json
{
  "status": "RECUSADO",
  "message": "Saldo insuficiente"
}
```

## Configuração do Banco de Dados

A aplicação utiliza banco H2 em memória:
- **Console H2**: http://localhost:8080/h2-console
- **URL**: jdbc:h2:mem:testdb
- **Usuário**: sa
- **Senha**: password

## Como Executar

1. Certifique-se de ter Java 17 instalado
2. Execute o comando: `mvn spring-boot:run`
3. A API estará disponível em: http://localhost:8080
4. Acesse a documentação Swagger em: http://localhost:8080/swagger-ui.html

## Exemplos de Uso

### Criar uma conta
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Santos",
    "cpf": "98765432100",
    "birthDate": "1985-03-20",
    "email": "maria@email.com",
    "phone": "11999888777"
  }'
```

### Creditar conta
```bash
curl -X POST http://localhost:8080/api/accounts/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 500.00
  }'
```

### Debitar conta
```bash
curl -X POST http://localhost:8080/api/accounts/debit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "amount": 100.00
  }'
```

### Consultar conta
```bash
curl http://localhost:8080/api/accounts/1
```

## Controle de Versão

A entidade Account possui controle de versão otimista através do campo `version`, que é incrementado automaticamente a cada atualização para evitar conflitos de concorrência.