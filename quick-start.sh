
#!/bin/bash

echo "🏦 Banking System - Quick Start Script"
echo "======================================"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check Docker
if ! command_exists docker; then
    echo "❌ Docker is not installed. Please install Docker Desktop first."
    echo "   Download: https://www.docker.com/products/docker-desktop/"
    exit 1
fi

# Check Docker Compose
if ! command_exists docker-compose; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose."
    exit 1
fi

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop first."
    exit 1
fi

echo "✅ Docker is available and running"

# Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down --remove-orphans

# Build and start services
echo "🚀 Building and starting all services..."
docker-compose up --build -d

# Wait for services to be ready
echo "⏳ Waiting for services to initialize..."

# Wait for PostgreSQL
echo "📊 Waiting for PostgreSQL..."
until docker-compose exec -T postgres pg_isready -U banking_user -d banking_system; do
    echo "   PostgreSQL is not ready yet..."
    sleep 5
done
echo "✅ PostgreSQL is ready"

# Wait for Kafka
echo "📡 Waiting for Kafka..."
until docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; do
    echo "   Kafka is not ready yet..."
    sleep 5
done
echo "✅ Kafka is ready"

# Wait for Banking Application
echo "🏦 Waiting for Banking Application..."
timeout=300  # 5 minutes timeout
counter=0
while [ $counter -lt $timeout ]; do
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "✅ Banking Application is ready!"
        break
    fi
    echo "   Banking Application is starting... (${counter}s/${timeout}s)"
    sleep 10
    counter=$((counter + 10))
done

if [ $counter -ge $timeout ]; then
    echo "⚠️ Banking Application took too long to start. Check logs:"
    echo "   docker-compose logs banking-app"
else
    echo ""
    echo "🎉 All services are ready!"
    echo ""
    echo "🌐 Available Services:"
    echo "┌─────────────────────────────────────────────────────────────┐"
    echo "│ Service                │ URL                                │"
    echo "├─────────────────────────────────────────────────────────────┤"
    echo "│ Banking API            │ http://localhost:8080              │"
    echo "│ Swagger Documentation  │ http://localhost:8080/swagger-ui.html │"
    echo "│ API Gateway            │ http://localhost:8080/api/gateway  │"
    echo "│ Performance Metrics    │ http://localhost:8080/api/performance │"
    echo "│ Health Check           │ http://localhost:8080/actuator/health │"
    echo "│ PostgreSQL Admin       │ http://localhost:8081              │"
    echo "│ Kafka UI               │ certo              │"
    echo "│ Management Port        │ http://localhost:8083/actuator     │"
    echo "│ Zipkin Tracing         │ http://localhost:9411              │"
    echo "└─────────────────────────────────────────────────────────────┘"
    echo ""
    echo "📊 Credentials:"
    echo "• PgAdmin: admin@banking.com / admin123"
    echo "• PostgreSQL: banking_user / banking_password"
    echo ""
    echo "🧪 Quick Test:"
    echo "curl -X POST http://localhost:8080/api/gateway/accounts \\"
    echo "  -H 'Content-Type: application/json' \\"
    echo "  -d '{\"name\":\"Test User\",\"cpf\":\"12345678901\",\"birthDate\":\"1990-01-01\"}'"
    echo ""
    echo "📚 To stop all services:"
    echo "docker-compose down"
    echo ""
    echo "📋 To view logs:"
    echo "docker-compose logs [service-name]"
fi