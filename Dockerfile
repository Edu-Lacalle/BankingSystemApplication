# Use OpenJDK 17 as base image with performance optimizations
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests

# Create non-root user for security
RUN groupadd -r banking && useradd -r -g banking banking
RUN chown -R banking:banking /app
USER banking

# Expose port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with performance optimizations
CMD ["java", \
     "-Xms512m", \
     "-Xmx2g", \
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=100", \
     "-XX:+HeapDumpOnOutOfMemoryError", \
     "-XX:HeapDumpPath=/app/heapdump.hprof", \
     "-Dspring.profiles.active=datadog", \
     "-jar", "target/BankingSystemApplication-0.0.1-SNAPSHOT.jar"]