#!/bin/bash

# Banking System - Datadog APM Agent Startup Script
# Este script inicia a aplicação bancária com o agente APM do Datadog

echo "🚀 Starting Banking System with Datadog APM..."

# Set default Datadog API key if not provided
if [ -z "$DD_API_KEY" ]; then
    echo "📡 Using default Datadog API key from configuration"
    export DD_API_KEY="c576fa9380cc70a22dee72e7df176697"
    export DD_SITE="us5.datadoghq.com"
fi

# Baixar o agente APM do Datadog se não existir
DATADOG_AGENT_JAR="dd-java-agent.jar"

if [ ! -f "$DATADOG_AGENT_JAR" ]; then
    echo "📥 Downloading Datadog Java APM Agent..."
    curl -Lo $DATADOG_AGENT_JAR 'https://dtdg.co/latest-java-tracer'
    
    if [ $? -eq 0 ]; then
        echo "✅ Datadog Agent downloaded successfully"
    else
        echo "❌ Failed to download Datadog Agent"
        echo "   Running without APM agent..."
        DATADOG_AGENT_JAR=""
    fi
fi

# Configurações padrão do Datadog
export DD_SERVICE=${DD_SERVICE:-"banking-system"}
export DD_ENV=${DD_ENV:-"local"}
export DD_VERSION=${DD_VERSION:-"1.0.0"}
export DD_LOGS_INJECTION=${DD_LOGS_INJECTION:-"true"}
export DD_TRACE_SAMPLE_RATE=${DD_TRACE_SAMPLE_RATE:-"1"}
export DD_PROFILING_ENABLED=${DD_PROFILING_ENABLED:-"true"}
export DD_TRACE_ENABLED=${DD_TRACE_ENABLED:-"true"}

# Configurações para agente local do Datadog
export DD_AGENT_HOST=${DD_AGENT_HOST:-"localhost"}
export DD_TRACE_AGENT_PORT=${DD_TRACE_AGENT_PORT:-"8126"}
export DD_DOGSTATSD_PORT=${DD_DOGSTATSD_PORT:-"8125"}

# Configurações específicas para aplicação bancária
export DD_SERVICE_MAPPING="postgresql:banking-db,kafka:banking-events"
export DD_TAGS="team:backend,domain:banking,architecture:hexagonal"

# JVM Options
JVM_OPTS="-Xms512m -Xmx2g"
JVM_OPTS="$JVM_OPTS -Dspring.profiles.active=datadog"
JVM_OPTS="$JVM_OPTS -Dmanagement.metrics.export.datadog.enabled=true"

# Adicionar o agente do Datadog se disponível
if [ ! -z "$DATADOG_AGENT_JAR" ] && [ -f "$DATADOG_AGENT_JAR" ]; then
    echo "🔍 Starting with Datadog APM Agent enabled"
    JVM_OPTS="$JVM_OPTS -javaagent:$DATADOG_AGENT_JAR"
else
    echo "📊 Starting with Datadog metrics only (no APM agent)"
fi

echo ""
echo "🔧 Configuration:"
echo "   Service: $DD_SERVICE"
echo "   Environment: $DD_ENV" 
echo "   Version: $DD_VERSION"
echo "   APM Agent: $([ ! -z "$DATADOG_AGENT_JAR" ] && [ -f "$DATADOG_AGENT_JAR" ] && echo "Enabled" || echo "Disabled")"
echo "   Profiling: $DD_PROFILING_ENABLED"
echo "   Log Injection: $DD_LOGS_INJECTION"
echo ""

# Iniciar a aplicação
echo "🏦 Starting Banking System Application..."

java $JVM_OPTS -jar target/BankingSystemApplication-0.0.1-SNAPSHOT.jar

echo "👋 Banking System stopped."