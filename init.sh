#!/bin/bash
#
# Development Environment Initialization Script
# This script starts the development server and verifies basic functionality
# Based on: https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents
#

set -e

echo "=== Initializing Appointment System Development Environment ==="

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "Error: pom.xml not found. Please run this script from the project root."
    exit 1
fi

echo "[1/4] Checking Java version..."
java -version

echo "[2/4] Building project with Maven..."
./mvnw clean compile -DskipTests

echo "[3/4] Starting development server..."
echo "Starting Spring Boot application..."
./mvnw spring-boot:run &

# Wait for server to start
echo "[4/4] Waiting for server to be ready..."
MAX_WAIT=60
WAIT_COUNT=0
while ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    WAIT_COUNT=$((WAIT_COUNT + 1))
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        echo "Warning: Server health check timed out after ${MAX_WAIT} seconds"
        echo "Server may still be starting..."
        break
    fi
    sleep 1
    echo -n "."
done
echo ""

if [ $WAIT_COUNT -lt $MAX_WAIT ]; then
    echo "Server is ready at http://localhost:8080"
fi

echo "=== Development Environment Ready ==="
echo ""
echo "API endpoints:"
echo "  - Health check: http://localhost:8080/actuator/health"
echo "  - API base: http://localhost:8080/api"
echo ""
echo "To stop the server: ./stop-dev.sh or kill \$(lsof -t -i:8080)"
