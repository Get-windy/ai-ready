#!/bin/bash
# AI-Ready API Service Startup Script
# Usage: ./start-api-service.sh [port]

set -e

PORT=${1:-8080}
JAR_FILE="target/ai-ready-minimal-1.0.0-SNAPSHOT.jar"
LOG_DIR="../logs"
LOG_FILE="$LOG_DIR/api-service.log"

echo "Starting AI-Ready API Service..."
echo "Port: $PORT"
echo "Log: $LOG_FILE"

# Create log directory if not exists
mkdir -p "$LOG_DIR"

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please build the project first: mvn clean package"
    exit 1
fi

# Stop existing service on the same port
PID=$(lsof -ti:$PORT 2>/dev/null || true)
if [ -n "$PID" ]; then
    echo "Stopping existing service on port $PORT (PID: $PID)..."
    kill -9 $PID 2>/dev/null || true
    sleep 2
fi

# Start the service
nohup java -jar "$JAR_FILE" \
    --server.port=$PORT \
    --spring.profiles.active=dev \
    > "$LOG_FILE" 2>&1 &

NEW_PID=$!
echo "Service started with PID: $NEW_PID"

# Wait for service to be ready
echo "Waiting for service to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:$PORT/actuator/health > /dev/null 2>&1; then
        echo "Service is ready!"
        echo "Health check: http://localhost:$PORT/actuator/health"
        exit 0
    fi
    sleep 1
done

echo "Warning: Service may not be fully started yet. Check logs: $LOG_FILE"
exit 1
