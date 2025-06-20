#!/bin/bash

echo "Starting Banking System Application with Docker..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker Desktop first."
    echo "If you're on Windows/WSL2, make sure Docker Desktop is installed and running."
    echo "You can download it from: https://www.docker.com/products/docker-desktop/"
    exit 1
fi

echo "Docker is running. Starting containers..."

# Remove old containers if they exist
echo "Removing old containers..."
docker-compose down

# Build and start all services
echo "Building and starting all services..."
docker-compose up --build -d

# Wait a moment for services to initialize
echo "Waiting for services to initialize..."
sleep 10

# Show container status
echo "Container status:"
docker-compose ps

# Show application logs
echo "Banking application logs:"
docker-compose logs banking-app

echo ""
echo "Services started successfully!"
echo ""
echo "Available services:"
echo "- Banking Application: http://localhost:8080"
echo "- Swagger UI: http://localhost:8080/swagger-ui.html"
echo "- PgAdmin: http://localhost:8081 (admin@banking.com / admin123)"
echo "- Kafka UI: http://localhost:8082"
echo "- Management/Actuator: http://localhost:8083"
echo "- Zipkin Tracing: http://localhost:9411"
echo ""
echo "To stop all services: docker-compose down"
echo "To view logs: docker-compose logs [service-name]"